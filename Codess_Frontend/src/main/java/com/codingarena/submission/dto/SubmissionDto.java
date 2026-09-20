package com.codingarena.submission.dto;

import com.codingarena.submission.model.Submission;

import java.time.LocalDateTime;
import java.util.UUID;

public class SubmissionDto {

    private UUID id;
    private UUID matchId;
    private UUID problemId;
    private UUID userId;
    private String username;
    private String code;
    private String language;
    private String verdict;
    private LocalDateTime submittedAt;

    public SubmissionDto() {
    }

    public static SubmissionDto fromEntity(Submission submission) {
        SubmissionDto dto = new SubmissionDto();
        dto.setId(submission.getId());
        dto.setMatchId(submission.getMatch() != null ? submission.getMatch().getId() : null);
        if (submission.getProblem() != null) {
            dto.setProblemId(submission.getProblem().getId());
        } else if (submission.getMatch() != null && submission.getMatch().getProblem() != null) {
            dto.setProblemId(submission.getMatch().getProblem().getId());
        }
        dto.setUserId(submission.getUser().getId());
        dto.setUsername(submission.getUser().getUsername());
        dto.setCode(submission.getCode());
        dto.setLanguage(submission.getLanguage());
        dto.setVerdict(submission.getVerdict());
        dto.setSubmittedAt(submission.getSubmittedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public void setProblemId(UUID problemId) {
        this.problemId = problemId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
