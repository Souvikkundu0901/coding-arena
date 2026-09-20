package com.codingarena.friendship.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendFriendRequest {

    @NotBlank(message = "Username is required")
    @JsonAlias({"username", "targetUsername"})
    private String username;

    public SendFriendRequest() {
    }

    public SendFriendRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
