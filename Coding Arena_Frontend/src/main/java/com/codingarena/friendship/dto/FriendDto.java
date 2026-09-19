package com.codingarena.friendship.dto;

import com.codingarena.auth.model.User;

import java.util.UUID;

public class FriendDto {

    private UUID id;
    private String username;
    private Integer rating;

    public FriendDto() {
    }

    public FriendDto(UUID id, String username, Integer rating) {
        this.id = id;
        this.username = username;
        this.rating = rating;
    }

    public static FriendDto fromEntity(User user) {
        if (user == null) return null;
        return new FriendDto(user.getId(), user.getUsername(), user.getRating());
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
