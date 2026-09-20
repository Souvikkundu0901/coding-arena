package com.codingarena.friendship.controller;

import com.codingarena.auth.model.User;
import com.codingarena.friendship.dto.FriendDto;
import com.codingarena.friendship.dto.FriendRequestDto;
import com.codingarena.friendship.dto.SendFriendRequest;
import com.codingarena.friendship.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(
            @Valid @RequestBody SendFriendRequest request,
            @AuthenticationPrincipal User currentUser) {
        FriendRequestDto dto = friendshipService.sendFriendRequest(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<FriendRequestDto>> getPendingIncomingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendshipService.getPendingIncomingRequests(currentUser));
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<FriendRequestDto> acceptFriendRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendshipService.acceptFriendRequest(id, currentUser));
    }

    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<FriendRequestDto> declineFriendRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendshipService.declineFriendRequest(id, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendshipService.getFriends(currentUser));
    }
}
