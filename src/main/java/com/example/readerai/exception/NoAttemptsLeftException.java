package com.example.readerai.exception;

// Учень вичерпав спроби на фрагмент, призначений класу (HTTP 409).
public class NoAttemptsLeftException extends RuntimeException {
    public NoAttemptsLeftException(String message) {
        super(message);
    }
}
