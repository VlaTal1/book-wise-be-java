package com.example.readerai.controller;

import com.example.readerai.dto.BookDTO;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Немає класового @RequestMapping навмисно (як і в ReadingSpeedAttemptController)
// — щоб шлях був саме "/internal/**" (permitAll у SecurityConfig, без Supabase
// JWT), а не під префіксом "/api/books" з BookController, де вимагається JWT.
@RestController
@RequiredArgsConstructor
public class InternalBookController {

    private final BookService bookService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // Викликається лише Python-сервісом при старті сесії читання, щоб
    // випадково обрати уривок з книги, призначеної дитині (Access) —
    // service-to-service, той самий API-ключ, що і в
    // ReadingSpeedAttemptController.
    @GetMapping("/internal/participants/{participantId}/books")
    public ResponseEntity<List<BookDTO>> getBooksForParticipant(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @PathVariable Long participantId
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new PermissionDeniedException("Invalid internal API key");
        }
        return ResponseEntity.ok(bookService.getAllBooksByParticipantId(participantId));
    }
}
