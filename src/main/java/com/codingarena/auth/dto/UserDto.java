package com.codingarena.auth.dto;

import com.codingarena.auth.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private Integer rating;
    private Integer wins;
    private Integer losses;
    private LocalDateTime createdAt;

    public UserDto() {
    }

    public UserDto(UUID id, String username, String email, Integer rating, Integer wins, Integer losses, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.rating = rating;
        this.wins = wins;
        this.losses = losses;
        this.createdAt = createdAt;
    }

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRating(),
                user.getWins() != null ? user.getWins() : 0,
                user.getLosses() != null ? user.getLosses() : 0,
                user.getCreatedAt()
        );
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
