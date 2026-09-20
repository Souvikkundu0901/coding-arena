package com.codingarena.match.dto;

import com.codingarena.match.model.Match;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MatchDto {

    private UUID id;
    private UUID playerAId;
    private String playerAUsername;
    private Integer playerARating;
    private UUID playerBId;
    private String playerBUsername;
    private Integer playerBRating;
    private ProblemDto problem;
    private List<PlayerSummaryDto> players = new java.util.ArrayList<>();
    private String status;
    private UUID winnerId;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime endedAt;

    public MatchDto() {
    }

    public static MatchDto fromEntity(Match match) {
        MatchDto dto = new MatchDto();
        dto.setId(match.getId());
        dto.setPlayerAId(match.getPlayerA().getId());
        dto.setPlayerAUsername(match.getPlayerA().getUsername());
        dto.setPlayerARating(match.getPlayerA().getRating());
        dto.setPlayerBId(match.getPlayerB().getId());
        dto.setPlayerBUsername(match.getPlayerB().getUsername());
        dto.setPlayerBRating(match.getPlayerB().getRating());

        dto.setPlayers(java.util.List.of(
                new PlayerSummaryDto(match.getPlayerA().getId(), match.getPlayerA().getUsername(), match.getPlayerA().getRating()),
                new PlayerSummaryDto(match.getPlayerB().getId(), match.getPlayerB().getUsername(), match.getPlayerB().getRating())
        ));

        dto.setProblem(ProblemDto.fromEntity(match.getProblem()));
        dto.setStatus(match.getStatus());
        dto.setWinnerId(match.getWinnerId());
        dto.setStartedAt(match.getStartedAt());
        dto.setExpiresAt(match.getExpiresAt());
        dto.setEndedAt(match.getEndedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMatchId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlayerAId() {
        return playerAId;
    }

    public void setPlayerAId(UUID playerAId) {
        this.playerAId = playerAId;
    }

    public String getPlayerAUsername() {
        return playerAUsername;
    }

    public void setPlayerAUsername(String playerAUsername) {
        this.playerAUsername = playerAUsername;
    }

    public Integer getPlayerARating() {
        return playerARating;
    }

    public void setPlayerARating(Integer playerARating) {
        this.playerARating = playerARating;
    }

    public UUID getPlayerBId() {
        return playerBId;
    }

    public void setPlayerBId(UUID playerBId) {
        this.playerBId = playerBId;
    }

    public String getPlayerBUsername() {
        return playerBUsername;
    }

    public void setPlayerBUsername(String playerBUsername) {
        this.playerBUsername = playerBUsername;
    }

    public Integer getPlayerBRating() {
        return playerBRating;
    }

    public void setPlayerBRating(Integer playerBRating) {
        this.playerBRating = playerBRating;
    }

    public ProblemDto getProblem() {
        return problem;
    }

    public void setProblem(ProblemDto problem) {
        this.problem = problem;
    }

    public List<PlayerSummaryDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerSummaryDto> players) {
        this.players = players != null ? players : new java.util.ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
