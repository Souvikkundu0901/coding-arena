package com.codingarena.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "theme", nullable = false)
    private String theme = "dark";

    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = true;

    public UserPreferences() {
    }

    public UserPreferences(UUID userId) {
        this.userId = userId;
        this.theme = "dark";
        this.emailNotifications = true;
    }

    public UserPreferences(UUID userId, String theme, Boolean emailNotifications) {
        this.userId = userId;
        this.theme = theme != null ? theme : "dark";
        this.emailNotifications = emailNotifications != null ? emailNotifications : true;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
