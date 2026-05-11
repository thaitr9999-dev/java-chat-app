package com.kimtruong.chat_app.exception;

/**
 * Custom exception for chat application business logic errors.
 * Allows us to distinguish app-specific errors from framework errors.
 */
public class ChatAppException extends RuntimeException {

    public ChatAppException(String message) {
        super(message);
    }

    public ChatAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
