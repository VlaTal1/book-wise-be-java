package com.example.readerai.controller;

import com.example.readerai.dto.ReadingSpeedAttemptDTO;
import com.example.readerai.dto.StressProgressUpdateDTO;
import com.example.readerai.dto.StressResultUpdateDTO;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.service.ReadingSpeedAttemptService;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
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

    // Дитячий режим — історія лише цієї дитини (ownership-check у сервісі).
    @GetMapping("/api/reading-speed-attempts/participant/{participantId}")
    public ResponseEntity<List<ReadingSpeedAttemptDTO>> getHistory(@PathVariable Long participantId) {
        return new ResponseEntity<>(readingSpeedAttemptService.getHistory(participantId), HttpStatus.OK);
    }

    // Батьківський режим — історія по УСІХ дітях поточного користувача
    // одним списком (participantName у кожному елементі показує, чия це
    // спроба).
    @GetMapping("/api/reading-speed-attempts")
    public ResponseEntity<List<ReadingSpeedAttemptDTO>> getHistoryForCurrentUser() {
        return new ResponseEntity<>(readingSpeedAttemptService.getHistoryForCurrentUser(), HttpStatus.OK);
    }

    // Мобілка опитує цей ендпоінт з екрана результатів чтения, поки триває
    // шар перевірки наголосу (stressStatus PENDING/PROCESSING) — щоб
    // показати лоадер/прогрес-бар і, коли готово, фінальну точність наголосу.
    // Той самий ендпоінт використовує й екран деталей історії читання.
    @GetMapping("/api/reading-speed-attempts/{id}")
    public ResponseEntity<ReadingSpeedAttemptDTO> getById(@PathVariable Long id) {
        return new ResponseEntity<>(readingSpeedAttemptService.getById(id), HttpStatus.OK);
    }

    // Видалення запису історії (разом з аудіо в MinIO, якщо воно є) —
    // ownership-check у сервісі, той самий, що й у getById.
    @DeleteMapping("/api/reading-speed-attempts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        readingSpeedAttemptService.deleteAttempt(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Аудіо сесії читання (для програвання на екрані деталей історії) —
    // завантажене Python-сервісом у MinIO при збереженні attempt.
    @GetMapping("/api/reading-speed-attempts/{id}/audio")
    public ResponseEntity<InputStreamResource> getAudio(@PathVariable Long id) throws MinioException {
        InputStream audio = readingSpeedAttemptService.getAudioStream(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(new InputStreamResource(audio));
    }

    // Проміжні оновлення прогресу шару перевірки наголосу від Python-сервісу.
    @PatchMapping("/internal/reading-speed-attempts/{id}/stress-progress")
    public ResponseEntity<Void> updateStressProgress(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @PathVariable Long id,
            @RequestBody StressProgressUpdateDTO dto
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new PermissionDeniedException("Invalid internal API key");
        }
        readingSpeedAttemptService.updateStressProgress(id, dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Фінальний результат шару перевірки наголосу від Python-сервісу.
    @PatchMapping("/internal/reading-speed-attempts/{id}/stress-result")
    public ResponseEntity<Void> updateStressResult(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @PathVariable Long id,
            @RequestBody StressResultUpdateDTO dto
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new PermissionDeniedException("Invalid internal API key");
        }
        readingSpeedAttemptService.updateStressResult(id, dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
