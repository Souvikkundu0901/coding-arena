package com.codingarena.challenge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateChallengeRequest {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    public CreateChallengeRequest() {
    }

    public CreateChallengeRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
