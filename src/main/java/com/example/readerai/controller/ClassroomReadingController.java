package com.example.readerai.controller;

import com.example.readerai.dto.reading.ClassroomReadingDTO;
import com.example.readerai.dto.reading.ClassroomReadingRequest;
import com.example.readerai.dto.reading.ClassroomReadingResultsDTO;
import com.example.readerai.dto.reading.ReadingSessionDTO;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.service.ClassroomReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Фрагменти для читання на швидкість, призначені класу. Без класового
// @RequestMapping — тут і /api/** (JWT), і /internal/** (API-ключ для Python).
@RestController
@RequiredArgsConstructor
public class ClassroomReadingController {

    private final ClassroomReadingService classroomReadingService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // Абзаци чистого тексту сторінок — вчитель обирає перший і останній.
    @GetMapping("/api/classrooms/{id}/reading-preview")
    public ResponseEntity<List<String>> preview(
            @PathVariable Long id,
            @RequestParam Long bookId,
            @RequestParam Integer startPage,
            @RequestParam Integer endPage
    ) {
        return ResponseEntity.ok(classroomReadingService.preview(id, bookId, startPage, endPage));
    }

    @PostMapping("/api/classrooms/{id}/readings")
    public ResponseEntity<ClassroomReadingDTO> create(@PathVariable Long id, @RequestBody ClassroomReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classroomReadingService.create(id, request));
    }

    @GetMapping("/api/classrooms/{id}/readings/{readingId}/results")
    public ResponseEntity<ClassroomReadingResultsDTO> getResults(@PathVariable Long id, @PathVariable Long readingId) {
        return ResponseEntity.ok(classroomReadingService.getResults(id, readingId));
    }

    @DeleteMapping("/api/classrooms/{id}/readings/{readingId}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long readingId) {
        classroomReadingService.delete(id, readingId);
        return ResponseEntity.noContent().build();
    }

    // Учень — завдання свого класу з використаними спробами й найкращою оцінкою.
    @GetMapping("/api/classroom-readings/my")
    public ResponseEntity<List<ClassroomReadingDTO>> getMine() {
        return ResponseEntity.ok(classroomReadingService.getForCurrentStudent());
    }

    // Python на старті сесії читання: текст, ліміт часу; 409 — спроби вичерпано.
    @GetMapping("/internal/classroom-readings/{readingId}/session")
    public ResponseEntity<ReadingSessionDTO> getSession(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @PathVariable Long readingId,
            @RequestParam Long participantId
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new PermissionDeniedException("Invalid internal API key");
        }
        return ResponseEntity.ok(classroomReadingService.getSession(readingId, participantId));
    }
}
