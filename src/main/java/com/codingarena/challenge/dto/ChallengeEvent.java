package com.codingarena.challenge.dto;

import java.time.LocalDateTime;

public class ChallengeEvent {

    private String type; // CHALLENGE_RECEIVED / CHALLENGE_DECLINED
    private ChallengeDto challenge;
    private LocalDateTime timestamp;

    public ChallengeEvent() {
    }

    public ChallengeEvent(String type, ChallengeDto challenge) {
        this.type = type;
        this.challenge = challenge;
        this.timestamp = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChallengeDto getChallenge() {
        return challenge;
    }

    public void setChallenge(ChallengeDto challenge) {
        this.challenge = challenge;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
