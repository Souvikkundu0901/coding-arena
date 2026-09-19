package com.codingarena.friendship.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.challenge.exception.UserNotFoundException;
import com.codingarena.friendship.dto.FriendDto;
import com.codingarena.friendship.dto.FriendRequestDto;
import com.codingarena.friendship.dto.SendFriendRequest;
import com.codingarena.friendship.exception.FriendRequestAccessDeniedException;
import com.codingarena.friendship.exception.FriendRequestConflictException;
import com.codingarena.friendship.exception.FriendRequestNotFoundException;
import com.codingarena.friendship.exception.SelfFriendRequestException;
import com.codingarena.friendship.model.Friendship;
import com.codingarena.friendship.repository.FriendshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    private static final Logger log = LoggerFactory.getLogger(FriendshipService.class);

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FriendRequestDto sendFriendRequest(SendFriendRequest request, User currentUser) {
        String targetUsername = request.getUsername() != null ? request.getUsername().trim() : "";

        User addressee = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUsername));

        if (Boolean.TRUE.equals(addressee.getIsDeleted())) {
            throw new UserNotFoundException("User not found: " + targetUsername);
        }

        if (currentUser.getId().equals(addressee.getId())) {
            throw new SelfFriendRequestException("Cannot send a friend request to yourself");
        }

        if (friendshipRepository.existsPendingOrAcceptedFriendshipBetween(currentUser.getId(), addressee.getId())) {
            throw new FriendRequestConflictException("A pending or accepted friendship already exists between these users");
        }

        Friendship friendship = new Friendship(currentUser, addressee);
        Friendship saved = friendshipRepository.save(friendship);

        log.info("User {} sent friend request to user {} (id: {})", currentUser.getUsername(), addressee.getUsername(), saved.getId());
        return FriendRequestDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> getPendingIncomingRequests(User currentUser) {
        return friendshipRepository.findPendingIncomingByAddresseeId(currentUser.getId())
                .stream()
                .map(FriendRequestDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FriendRequestDto acceptFriendRequest(UUID requestId, User currentUser) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found: " + requestId));

        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new FriendRequestAccessDeniedException("Only the recipient can accept this friend request");
        }

        if (!"PENDING".equals(friendship.getStatus())) {
            throw new FriendRequestConflictException("Friend request is no longer pending");
        }

        int updated = friendshipRepository.acceptFriendshipAtomically(requestId);
        if (updated == 0) {
            throw new FriendRequestConflictException("Friend request could not be accepted because it is no longer pending");
        }

        friendship.setStatus("ACCEPTED");
        log.info("User {} accepted friend request {}", currentUser.getUsername(), requestId);
        return FriendRequestDto.fromEntity(friendship);
    }

    @Transactional
    public FriendRequestDto declineFriendRequest(UUID requestId, User currentUser) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found: " + requestId));

        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new FriendRequestAccessDeniedException("Only the recipient can decline this friend request");
        }

        if (!"PENDING".equals(friendship.getStatus())) {
            throw new FriendRequestConflictException("Friend request is no longer pending");
        }

        int updated = friendshipRepository.declineFriendshipAtomically(requestId);
        if (updated == 0) {
            throw new FriendRequestConflictException("Friend request could not be declined because it is no longer pending");
        }

        friendship.setStatus("DECLINED");
        log.info("User {} declined friend request {}", currentUser.getUsername(), requestId);
        return FriendRequestDto.fromEntity(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getFriends(User currentUser) {
        List<Friendship> accepted = friendshipRepository.findAcceptedFriendshipsByUserId(currentUser.getId());
        return accepted.stream().map(f -> {
            User friend = f.getRequester().getId().equals(currentUser.getId())
                    ? f.getAddressee()
                    : f.getRequester();
            return FriendDto.fromEntity(friend);
        }).collect(Collectors.toList());
    }
}
