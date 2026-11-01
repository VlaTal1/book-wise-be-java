package com.example.readerai.exception;

// Зовнішній сервіс (Python) недоступний — HTTP 503.
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
