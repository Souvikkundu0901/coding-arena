package com.codingarena.match.controller;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.ProblemDto;
import com.codingarena.match.service.ProblemService;
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
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final SubmissionService submissionService;

    public ProblemController(ProblemService problemService, SubmissionService submissionService) {
        this.problemService = problemService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public ResponseEntity<List<ProblemDto>> getAllProblems() {
        return ResponseEntity.ok(problemService.getAllProblems());
    }

    @GetMapping("/{problemId}")
    public ResponseEntity<ProblemDto> getProblemById(@PathVariable UUID problemId) {
        return ResponseEntity.ok(problemService.getProblemById(problemId));
    }

    @PostMapping("/{problemId}/submissions")
    public ResponseEntity<SubmissionDto> createPracticeSubmission(
            @PathVariable UUID problemId,
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User currentUser) {
        SubmissionDto dto = submissionService.createPracticeSubmission(problemId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{problemId}/submissions/me")
    public ResponseEntity<List<SubmissionDto>> getMyPracticeSubmissions(
            @PathVariable UUID problemId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(submissionService.getPracticeSubmissions(problemId, currentUser));
    }
}
