package com.example.readerai.entity;

// Стан генерації питань тесту Python-сервісом (асинхронно через RabbitMQ).
public enum GenerationStatus {
    GENERATING,
    READY,
    FAILED
}
