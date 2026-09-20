package com.codingarena.auth.dto;

import com.codingarena.auth.model.UserPreferences;

public class UserPreferencesDto {

    private String theme;
    private Boolean emailNotifications;

    public UserPreferencesDto() {
    }

    public UserPreferencesDto(String theme, Boolean emailNotifications) {
        this.theme = theme;
        this.emailNotifications = emailNotifications;
    }

    public static UserPreferencesDto fromEntity(UserPreferences preferences) {
        if (preferences == null) {
            return new UserPreferencesDto("dark", true);
        }
        return new UserPreferencesDto(preferences.getTheme(), preferences.getEmailNotifications());
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
