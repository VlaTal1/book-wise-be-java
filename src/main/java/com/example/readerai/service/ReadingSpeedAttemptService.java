package com.example.readerai.service;

import com.example.readerai.converter.ReadingSpeedAttemptConverter;
import com.example.readerai.dto.ReadingSpeedAttemptDTO;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ParticipantRepository;
import com.example.readerai.repository.ReadingSpeedAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReadingSpeedAttemptService {

    private final ReadingSpeedAttemptRepository readingSpeedAttemptRepository;

    private final ParticipantRepository participantRepository;

    private final UserService userService;

    private final ReadingSpeedAttemptConverter readingSpeedAttemptConverter;

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
}
