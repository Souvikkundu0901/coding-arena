package com.codingarena.challenge.dto;

import com.codingarena.challenge.model.Challenge;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChallengeDto {

    private UUID id;
    private UUID challengerId;
    private String challengerUsername;
    private Integer challengerRating;
    private UUID challengedId;
    private String challengedUsername;
    private Integer challengedRating;
    private String status;
    private UUID matchId;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public ChallengeDto() {
    }

    public static ChallengeDto fromEntity(Challenge challenge) {
        ChallengeDto dto = new ChallengeDto();
        dto.setId(challenge.getId());
        if (challenge.getChallenger() != null) {
            dto.setChallengerId(challenge.getChallenger().getId());
            dto.setChallengerUsername(challenge.getChallenger().getUsername());
            dto.setChallengerRating(challenge.getChallenger().getRating());
        }
        if (challenge.getChallenged() != null) {
            dto.setChallengedId(challenge.getChallenged().getId());
            dto.setChallengedUsername(challenge.getChallenged().getUsername());
            dto.setChallengedRating(challenge.getChallenged().getRating());
        }
        dto.setStatus(challenge.getStatus());
        if (challenge.getMatch() != null) {
            dto.setMatchId(challenge.getMatch().getId());
        }
        dto.setCreatedAt(challenge.getCreatedAt());
        dto.setRespondedAt(challenge.getRespondedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getChallengerId() {
        return challengerId;
    }

    public void setChallengerId(UUID challengerId) {
        this.challengerId = challengerId;
    }

    public String getChallengerUsername() {
        return challengerUsername;
    }

    public String getFromUsername() {
        return challengerUsername;
    }

    public void setChallengerUsername(String challengerUsername) {
        this.challengerUsername = challengerUsername;
    }

    public Integer getChallengerRating() {
        return challengerRating;
    }

    public void setChallengerRating(Integer challengerRating) {
        this.challengerRating = challengerRating;
    }

    public UUID getChallengedId() {
        return challengedId;
    }

    public void setChallengedId(UUID challengedId) {
        this.challengedId = challengedId;
    }

    public String getChallengedUsername() {
        return challengedUsername;
    }

    public String getToUsername() {
        return challengedUsername;
    }

    public void setChallengedUsername(String challengedUsername) {
        this.challengedUsername = challengedUsername;
    }

    public Integer getChallengedRating() {
        return challengedRating;
    }

    public void setChallengedRating(Integer challengedRating) {
        this.challengedRating = challengedRating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
