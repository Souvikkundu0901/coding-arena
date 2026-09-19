package com.codingarena.friendship.exception;

public class SelfFriendRequestException extends RuntimeException {
    public SelfFriendRequestException(String message) {
        super(message);
    }
}
