package com.example.readerai.controller;

import com.example.readerai.dto.ReadingSpeedAttemptDTO;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.service.ReadingSpeedAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReadingSpeedAttemptController {

    private final ReadingSpeedAttemptService readingSpeedAttemptService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // Викликається лише Python-сервісом після завершення сесії читання —
    // service-to-service, захищено окремим API-ключем, а не Supabase JWT
    // (рішення по відкритому питанню в docs/reading-speed-feature-design.md,
    // розділ 5.2/8.2). Шлях /internal/** дозволено в SecurityConfig без JWT.
    @PostMapping("/internal/reading-speed-attempts")
    public ResponseEntity<ReadingSpeedAttemptDTO> save(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestBody ReadingSpeedAttemptDTO readingSpeedAttemptDTO
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new PermissionDeniedException("Invalid internal API key");
        }
        return new ResponseEntity<>(readingSpeedAttemptService.save(readingSpeedAttemptDTO), HttpStatus.CREATED);
    }

    @GetMapping("/api/reading-speed-attempts/participant/{participantId}")
    public ResponseEntity<List<ReadingSpeedAttemptDTO>> getHistory(@PathVariable Long participantId) {
        return new ResponseEntity<>(readingSpeedAttemptService.getHistory(participantId), HttpStatus.OK);
    }
}
