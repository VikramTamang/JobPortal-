package com.jobportal.exception;

public class InvalidApplicationStateException extends RuntimeException {
    public InvalidApplicationStateException(String message) {
        super(message);
    }
}
