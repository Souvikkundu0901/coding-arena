package com.codingarena.submission.controller;

import com.codingarena.auth.model.User;
import com.codingarena.submission.dto.SubmissionDto;
import com.codingarena.submission.dto.SubmissionRequest;
import com.codingarena.submission.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches/{matchId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionDto> createSubmission(
            @PathVariable UUID matchId,
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User currentUser) {
        SubmissionDto submissionDto = submissionService.createSubmission(matchId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionDto);
    }

    @GetMapping
    public ResponseEntity<List<SubmissionDto>> getSubmissions(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal User currentUser) {
        List<SubmissionDto> submissions = submissionService.getSubmissionsForMatch(matchId, currentUser);
        return ResponseEntity.ok(submissions);
    }
}
