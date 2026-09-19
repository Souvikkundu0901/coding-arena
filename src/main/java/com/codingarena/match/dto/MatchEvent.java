package com.codingarena.match.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchEvent {

    private String type; // MATCH_FOUND / MATCH_START / MATCH_END
    private MatchDto match;
    private String reason;
    private LocalDateTime timestamp;

    public MatchEvent() {
    }

    public MatchEvent(String type, MatchDto match) {
        this(type, match, null);
    }

    public MatchEvent(String type, MatchDto match, String reason) {
        this.type = type;
        this.match = match;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
