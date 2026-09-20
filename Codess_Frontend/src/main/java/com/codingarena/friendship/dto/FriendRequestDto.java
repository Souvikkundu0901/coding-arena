package com.codingarena.friendship.dto;

import com.codingarena.friendship.model.Friendship;

import java.time.LocalDateTime;
import java.util.UUID;

public class FriendRequestDto {

    private UUID id;
    private UUID requesterId;
    private String requesterUsername;
    private UUID addresseeId;
    private String addresseeUsername;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public FriendRequestDto() {
    }

    public FriendRequestDto(UUID id, UUID requesterId, String requesterUsername, UUID addresseeId, String addresseeUsername, String status, LocalDateTime createdAt, LocalDateTime respondedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterUsername = requesterUsername;
        this.addresseeId = addresseeId;
        this.addresseeUsername = addresseeUsername;
        this.status = status;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
    }

    public static FriendRequestDto fromEntity(Friendship friendship) {
        if (friendship == null) return null;
        return new FriendRequestDto(
                friendship.getId(),
                friendship.getRequester() != null ? friendship.getRequester().getId() : null,
                friendship.getRequester() != null ? friendship.getRequester().getUsername() : null,
                friendship.getAddressee() != null ? friendship.getAddressee().getId() : null,
                friendship.getAddressee() != null ? friendship.getAddressee().getUsername() : null,
                friendship.getStatus(),
                friendship.getCreatedAt(),
                friendship.getRespondedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public String getFromUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public UUID getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(UUID addresseeId) {
        this.addresseeId = addresseeId;
    }

    public String getAddresseeUsername() {
        return addresseeUsername;
    }

    public String getToUsername() {
        return addresseeUsername;
    }

    public void setAddresseeUsername(String addresseeUsername) {
        this.addresseeUsername = addresseeUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
