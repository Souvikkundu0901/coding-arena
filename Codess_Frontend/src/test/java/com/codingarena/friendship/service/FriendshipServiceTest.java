package com.codingarena.friendship.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.exception.UserNotFoundException;
import com.codingarena.friendship.dto.FriendDto;
import com.codingarena.friendship.dto.FriendRequestDto;
import com.codingarena.friendship.dto.SendFriendRequest;
import com.codingarena.friendship.exception.FriendRequestAccessDeniedException;
import com.codingarena.friendship.exception.FriendRequestConflictException;
import com.codingarena.friendship.exception.SelfFriendRequestException;
import com.codingarena.friendship.model.Friendship;
import com.codingarena.friendship.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    private FriendshipService friendshipService;

    private User requester;
    private User addressee;
    private UUID requesterId;
    private UUID addresseeId;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(friendshipRepository, userRepository);

        requesterId = UUID.randomUUID();
        requester = new User("requester", "req@example.com", "hash");
        requester.setId(requesterId);
        requester.setRating(1200);

        addresseeId = UUID.randomUUID();
        addressee = new User("addressee", "addr@example.com", "hash");
        addressee.setId(addresseeId);
        addressee.setRating(1250);
    }

    @Test
    void sendFriendRequest_Success() {
        SendFriendRequest request = new SendFriendRequest("addressee");
        when(userRepository.findByUsername("addressee")).thenReturn(Optional.of(addressee));
        when(friendshipRepository.existsPendingOrAcceptedFriendshipBetween(requesterId, addresseeId)).thenReturn(false);

        Friendship saved = new Friendship(requester, addressee);
        saved.setId(UUID.randomUUID());
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(saved);

        FriendRequestDto result = friendshipService.sendFriendRequest(request, requester);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("requester", result.getRequesterUsername());
        assertEquals("addressee", result.getAddresseeUsername());
    }

    @Test
    void sendFriendRequest_UserNotFound_ThrowsUserNotFoundException() {
        SendFriendRequest request = new SendFriendRequest("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> friendshipService.sendFriendRequest(request, requester));
    }

    @Test
    void sendFriendRequest_DeletedUser_ThrowsUserNotFoundException() {
        addressee.setIsDeleted(true);
        SendFriendRequest request = new SendFriendRequest("addressee");
        when(userRepository.findByUsername("addressee")).thenReturn(Optional.of(addressee));

        assertThrows(UserNotFoundException.class, () -> friendshipService.sendFriendRequest(request, requester));
    }

    @Test
    void sendFriendRequest_SelfRequest_ThrowsSelfFriendRequestException() {
        SendFriendRequest request = new SendFriendRequest("requester");
        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));

        assertThrows(SelfFriendRequestException.class, () -> friendshipService.sendFriendRequest(request, requester));
    }

    @Test
    void sendFriendRequest_DuplicatePendingOrAccepted_ThrowsFriendRequestConflictException() {
        SendFriendRequest request = new SendFriendRequest("addressee");
        when(userRepository.findByUsername("addressee")).thenReturn(Optional.of(addressee));
        when(friendshipRepository.existsPendingOrAcceptedFriendshipBetween(requesterId, addresseeId)).thenReturn(true);

        assertThrows(FriendRequestConflictException.class, () -> friendshipService.sendFriendRequest(request, requester));
    }

    @Test
    void getPendingIncomingRequests_ReturnsPendingOnlyForCaller() {
        Friendship f = new Friendship(requester, addressee);
        f.setId(UUID.randomUUID());

        when(friendshipRepository.findPendingIncomingByAddresseeId(addresseeId)).thenReturn(List.of(f));

        List<FriendRequestDto> result = friendshipService.getPendingIncomingRequests(addressee);

        assertEquals(1, result.size());
        assertEquals("requester", result.get(0).getRequesterUsername());
    }

    @Test
    void acceptFriendRequest_Success() {
        UUID requestId = UUID.randomUUID();
        Friendship f = new Friendship(requester, addressee);
        f.setId(requestId);

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(f));
        when(friendshipRepository.acceptFriendshipAtomically(requestId)).thenReturn(1);

        FriendRequestDto result = friendshipService.acceptFriendRequest(requestId, addressee);

        assertEquals("ACCEPTED", result.getStatus());
        verify(friendshipRepository).acceptFriendshipAtomically(requestId);
    }

    @Test
    void acceptFriendRequest_NotAddressee_ThrowsFriendRequestAccessDeniedException() {
        UUID requestId = UUID.randomUUID();
        Friendship f = new Friendship(requester, addressee);
        f.setId(requestId);

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(f));

        assertThrows(FriendRequestAccessDeniedException.class,
                () -> friendshipService.acceptFriendRequest(requestId, requester));
    }

    @Test
    void declineFriendRequest_Success() {
        UUID requestId = UUID.randomUUID();
        Friendship f = new Friendship(requester, addressee);
        f.setId(requestId);

        when(friendshipRepository.findById(requestId)).thenReturn(Optional.of(f));
        when(friendshipRepository.declineFriendshipAtomically(requestId)).thenReturn(1);

        FriendRequestDto result = friendshipService.declineFriendRequest(requestId, addressee);

        assertEquals("DECLINED", result.getStatus());
        verify(friendshipRepository).declineFriendshipAtomically(requestId);
    }

    @Test
    void getFriends_ReturnsFlatListOfOtherUsers() {
        Friendship f1 = new Friendship(requester, addressee);
        f1.setStatus("ACCEPTED");

        when(friendshipRepository.findAcceptedFriendshipsByUserId(requesterId)).thenReturn(List.of(f1));

        List<FriendDto> friends = friendshipService.getFriends(requester);

        assertEquals(1, friends.size());
        assertEquals(addresseeId, friends.get(0).getId());
        assertEquals("addressee", friends.get(0).getUsername());
        assertEquals(1250, friends.get(0).getRating());
    }
}
