package com.codingarena.match.dto;

import java.util.UUID;

public class PlayerSummaryDto {

    private UUID id;
    private String username;
    private Integer rating;

    public PlayerSummaryDto() {
    }

    public PlayerSummaryDto(UUID id, String username, Integer rating) {
        this.id = id;
        this.username = username;
        this.rating = rating;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
