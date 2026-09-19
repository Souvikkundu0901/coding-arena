package com.codingarena.match.controller;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatchDetails(@PathVariable UUID id,
                                                     @AuthenticationPrincipal User currentUser) {
        MatchDto matchDto = matchService.getMatchDetails(id, currentUser);
        return ResponseEntity.ok(matchDto);
    }
}
