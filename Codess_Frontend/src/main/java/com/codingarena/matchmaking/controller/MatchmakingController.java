package com.codingarena.matchmaking.controller;

import com.codingarena.auth.model.User;
import com.codingarena.matchmaking.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/queue")
    public ResponseEntity<Map<String, String>> joinQueue(@AuthenticationPrincipal User currentUser) {
        matchmakingService.joinQueue(currentUser);
        return ResponseEntity.ok(Map.of("message", "Joined matchmaking queue successfully"));
    }

    @DeleteMapping("/queue")
    public ResponseEntity<Map<String, String>> leaveQueue(@AuthenticationPrincipal User currentUser) {
        matchmakingService.leaveQueue(currentUser);
        return ResponseEntity.ok(Map.of("message", "Left matchmaking queue successfully"));
    }
}
