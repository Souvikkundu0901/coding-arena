package com.codingarena.auth.controller;

import com.codingarena.auth.dto.*;
import com.codingarena.auth.model.User;
import com.codingarena.auth.service.UserService;
import com.codingarena.match.dto.MatchHistoryDto;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MatchRepository matchRepository;
    private final UserService userService;

    public UserController(MatchRepository matchRepository, UserService userService) {
        this.matchRepository = matchRepository;
        this.userService = userService;
    }

    @GetMapping("/me/matches")
    public ResponseEntity<List<MatchHistoryDto>> getMyMatchHistory(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "4") int limit) {

        int pageSize = Math.max(1, limit);
        List<Match> matches = matchRepository.findCompletedOrExpiredMatchesByUserId(
                currentUser.getId(),
                PageRequest.of(0, pageSize)
        );

        List<MatchHistoryDto> history = matches.stream().map(match -> {
            boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(currentUser.getId());
            String opponentUsername = isPlayerA
                    ? (match.getPlayerB() != null ? match.getPlayerB().getUsername() : "Unknown")
                    : (match.getPlayerA() != null ? match.getPlayerA().getUsername() : "Unknown");

            String result;
            int ratingDelta = 0;

            if (match.getWinnerId() != null) {
                if (match.getWinnerId().equals(currentUser.getId())) {
                    result = "WIN";
                    ratingDelta = match.getWinnerRatingDelta() != null ? match.getWinnerRatingDelta() : 0;
                } else {
                    result = "LOSS";
                    ratingDelta = match.getLoserRatingDelta() != null ? match.getLoserRatingDelta() : 0;
                }
            } else if ("EXPIRED".equalsIgnoreCase(match.getStatus())) {
                result = "EXPIRED";
                ratingDelta = 0;
            } else {
                result = match.getStatus();
                ratingDelta = 0;
            }

            return new MatchHistoryDto(match.getId(), opponentUsername, result, ratingDelta);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserDto.fromEntity(currentUser));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateUsername(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUsernameRequest request) {
        return ResponseEntity.ok(userService.updateUsername(currentUser, request));
    }

    @PatchMapping("/me/email")
    public ResponseEntity<UserDto> updateEmail(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateEmailRequest request) {
        return ResponseEntity.ok(userService.updateEmail(currentUser, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<UserDto> updatePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(userService.updatePassword(currentUser, request));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesDto> getPreferences(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getPreferences(currentUser));
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<UserPreferencesDto> updatePreferences(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(currentUser, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(currentUser, request);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
