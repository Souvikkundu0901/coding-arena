package com.codingarena.match.dto;

import java.time.LocalDateTime;

public class MatchEvent {

    private String type; // MATCH_FOUND / MATCH_START / MATCH_END
    private MatchDto match;
    private LocalDateTime timestamp;

    public MatchEvent() {
    }

    public MatchEvent(String type, MatchDto match) {
        this.type = type;
        this.match = match;
        this.timestamp = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MatchDto getMatch() {
        return match;
    }

    public void setMatch(MatchDto match) {
        this.match = match;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
