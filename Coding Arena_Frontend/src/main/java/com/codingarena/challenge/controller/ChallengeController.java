package com.codingarena.challenge.controller;

import com.codingarena.auth.model.User;
import com.codingarena.challenge.dto.ChallengeDto;
import com.codingarena.challenge.dto.CreateChallengeRequest;
import com.codingarena.challenge.service.ChallengeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @PostMapping
    public ResponseEntity<ChallengeDto> createChallenge(@Valid @RequestBody CreateChallengeRequest request,
                                                        @AuthenticationPrincipal User currentUser) {
        ChallengeDto challenge = challengeService.createChallenge(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(challenge);
    }

    @PostMapping("/{challengeId}/accept")
    public ResponseEntity<ChallengeDto> acceptChallenge(@PathVariable UUID challengeId,
                                                        @AuthenticationPrincipal User currentUser) {
        ChallengeDto challenge = challengeService.acceptChallenge(challengeId, currentUser);
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/{challengeId}/decline")
    public ResponseEntity<ChallengeDto> declineChallenge(@PathVariable UUID challengeId,
                                                        @AuthenticationPrincipal User currentUser) {
        ChallengeDto challenge = challengeService.declineChallenge(challengeId, currentUser);
        return ResponseEntity.ok(challenge);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ChallengeDto>> getPendingChallenges(@AuthenticationPrincipal User currentUser) {
        List<ChallengeDto> pending = challengeService.getPendingChallenges(currentUser);
        return ResponseEntity.ok(pending);
    }
}
