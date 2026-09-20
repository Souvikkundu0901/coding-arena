package com.codingarena.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUsernameRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private String avatarSeed;

    public UpdateUsernameRequest() {
    }

    public UpdateUsernameRequest(String username) {
        this.username = username;
    }

    public UpdateUsernameRequest(String username, String avatarSeed) {
        this.username = username;
        this.avatarSeed = avatarSeed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarSeed() {
        return avatarSeed;
    }

    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = avatarSeed;
    }
}
