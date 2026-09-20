package com.codingarena.match.dto;

import java.util.UUID;

public class MatchHistoryDto {

    private UUID id;
    private String opponentUsername;
    private String result; // WIN / LOSS / EXPIRED
    private Integer ratingDelta;

    public MatchHistoryDto() {
    }

    public MatchHistoryDto(UUID id, String opponentUsername, String result, Integer ratingDelta) {
        this.id = id;
        this.opponentUsername = opponentUsername;
        this.result = result;
        this.ratingDelta = ratingDelta;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOpponentUsername() {
        return opponentUsername;
    }

    public void setOpponentUsername(String opponentUsername) {
        this.opponentUsername = opponentUsername;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getRatingDelta() {
        return ratingDelta;
    }

    public void setRatingDelta(Integer ratingDelta) {
        this.ratingDelta = ratingDelta;
    }
}
