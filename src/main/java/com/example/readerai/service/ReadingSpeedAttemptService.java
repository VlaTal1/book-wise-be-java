package com.example.readerai.service;

import com.example.readerai.converter.ReadingSpeedAttemptConverter;
import com.example.readerai.dto.ReadingSpeedAttemptDTO;
import com.example.readerai.dto.StressProgressUpdateDTO;
import com.example.readerai.dto.StressResultUpdateDTO;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ParticipantRepository;
import com.example.readerai.repository.ReadingSpeedAttemptRepository;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingSpeedAttemptService {

    private final ReadingSpeedAttemptRepository readingSpeedAttemptRepository;

    private final ParticipantRepository participantRepository;

    private final UserService userService;

    private final ReadingSpeedAttemptConverter readingSpeedAttemptConverter;

    private final MinioService minioService;

    // Викликається лише Python-сервісом через внутрішній ендпоінт (/internal/**,
    // захищений API-ключем, не Supabase JWT) — тут немає автентифікованого
    // користувача в контексті, тож перевірку власника учасника не робимо.
    public ReadingSpeedAttemptDTO save(ReadingSpeedAttemptDTO dto) {
        if (dto.getParticipantId() == null) {
            throw new IllegalArgumentException("Participant id cannot be null");
        }

        Participant participant = participantRepository.findById(dto.getParticipantId()).orElseThrow(
                () -> new NotFoundException("Participant not found")
        );

        ReadingSpeedAttempt attempt = readingSpeedAttemptConverter.fromDTO(dto);
        attempt.setParticipant(participant);

        ReadingSpeedAttempt savedAttempt = readingSpeedAttemptRepository.save(attempt);
        return readingSpeedAttemptConverter.toDTO(savedAttempt);
    }

    public List<ReadingSpeedAttemptDTO> getHistory(Long participantId) {
        Participant participant = participantRepository.findById(participantId).orElseThrow(
                () -> new NotFoundException("Participant not found")
        );
        if (!Objects.equals(participant.getUserId(), userService.getUserId())) {
            throw new PermissionDeniedException("User id mismatch");
        }

        return readingSpeedAttemptRepository.findAllByParticipant_IdOrderByCreatedAtDesc(participantId).stream()
                .map(readingSpeedAttemptConverter::toDTO)
                .toList();
    }

    // Батьківський режим — історія по УСІХ дітях поточного користувача
    // одним списком (participantName у DTO показує, чия це спроба).
    public List<ReadingSpeedAttemptDTO> getHistoryForCurrentUser() {
        return readingSpeedAttemptRepository.findAllByParticipant_UserIdOrderByCreatedAtDesc(userService.getUserId()).stream()
                .map(readingSpeedAttemptConverter::toDTO)
                .toList();
    }

    public ReadingSpeedAttemptDTO getById(Long id) {
        return readingSpeedAttemptConverter.toDTO(findOwnedAttempt(id));
    }

    // Стрім аудіо сесії читання для програвання в історії — той самий
    // ownership-check, що й getById, бо аудіо може містити голос дитини і
    // не повинно бути доступне нікому, крім власника (батька) цього профілю.
    public InputStream getAudioStream(Long id) throws MinioException {
        ReadingSpeedAttempt attempt = findOwnedAttempt(id);
        if (attempt.getAudioFileName() == null) {
            throw new NotFoundException("Audio not available for this attempt");
        }
        return minioService.getFile(attempt.getAudioFileName());
    }

    // Той самий ownership-check, що й getById/getAudioStream. Аудіо в MinIO
    // теж видаляємо (best-effort — якщо файл уже відсутній чи MinIO
    // недоступний, все одно видаляємо сам запис в БД, не блокуємо
    // користувача через проблему з побічним артефактом).
    public void deleteAttempt(Long id) {
        ReadingSpeedAttempt attempt = findOwnedAttempt(id);
        if (attempt.getAudioFileName() != null) {
            try {
                minioService.deleteFile(attempt.getAudioFileName());
            } catch (MinioException e) {
                log.warn("Failed to delete audio {} from MinIO for attempt {}: {}", attempt.getAudioFileName(), id, e.getMessage());
            }
        }
        readingSpeedAttemptRepository.delete(attempt);
    }

    private ReadingSpeedAttempt findOwnedAttempt(Long id) {
        ReadingSpeedAttempt attempt = readingSpeedAttemptRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Reading speed attempt not found")
        );
        if (!Objects.equals(attempt.getParticipant().getUserId(), userService.getUserId())) {
            throw new PermissionDeniedException("User id mismatch");
        }
        return attempt;
    }

    // Викликається лише Python-сервісом (/internal/**, API-ключ) під час
    // обробки шару перевірки наголосу — проміжні оновлення прогресу, щоб
    // мобілка могла показати прогрес-бар, поки триває forced alignment.
    public void updateStressProgress(Long id, StressProgressUpdateDTO dto) {
        ReadingSpeedAttempt attempt = readingSpeedAttemptRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Reading speed attempt not found")
        );
        attempt.setStressStatus(dto.getStatus());
        attempt.setStressProgress(dto.getProgress());
        readingSpeedAttemptRepository.save(attempt);
    }

    // Фінальний результат шару перевірки наголосу від Python.
    public void updateStressResult(Long id, StressResultUpdateDTO dto) {
        ReadingSpeedAttempt attempt = readingSpeedAttemptRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Reading speed attempt not found")
        );
        attempt.setStressStatus(dto.getStatus());
        attempt.setStressProgress(100);
        attempt.setStressAccuracy(dto.getAccuracy());
        attempt.setStressCheckedWords(dto.getCheckedWords());
        attempt.setStressCorrectWords(dto.getCorrectWords());
        attempt.setStressWordsJson(dto.getWordsJson());
        attempt.setStressError(dto.getError());
        readingSpeedAttemptRepository.save(attempt);
    }
}
