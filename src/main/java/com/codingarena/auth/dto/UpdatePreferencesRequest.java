package com.codingarena.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePreferencesRequest {

    private String theme;
    private Boolean emailNotifications;

    public UpdatePreferencesRequest() {
    }

    public UpdatePreferencesRequest(String theme, Boolean emailNotifications) {
        this.theme = theme;
        this.emailNotifications = emailNotifications;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
}
