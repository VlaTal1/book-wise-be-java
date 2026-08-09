package com.example.readerai.converter;

import com.example.readerai.dto.ReadingSpeedAttemptDTO;
import com.example.readerai.entity.ReadingSpeedAttempt;
import org.springframework.stereotype.Component;

@Component
public class ReadingSpeedAttemptConverter {

    public ReadingSpeedAttempt fromDTO(ReadingSpeedAttemptDTO entry) {
        return ReadingSpeedAttempt.builder()
                .id(entry.getId())
                .textId(entry.getTextId())
                .totalWords(entry.getTotalWords())
                .correctCount(entry.getCorrectCount())
                .errorCount(entry.getErrorCount())
                .skippedCount(entry.getSkippedCount())
                .notInVocabularyCount(entry.getNotInVocabularyCount())
                .durationSeconds(entry.getDurationSeconds())
                .wpm(entry.getWpm())
                .accuracy(entry.getAccuracy())
                .wordsJson(entry.getWordsJson())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    public ReadingSpeedAttemptDTO toDTO(ReadingSpeedAttempt entry) {
        return ReadingSpeedAttemptDTO.builder()
                .id(entry.getId())
                .participantId(entry.getParticipant().getId())
                .textId(entry.getTextId())
                .totalWords(entry.getTotalWords())
                .correctCount(entry.getCorrectCount())
                .errorCount(entry.getErrorCount())
                .skippedCount(entry.getSkippedCount())
                .notInVocabularyCount(entry.getNotInVocabularyCount())
                .durationSeconds(entry.getDurationSeconds())
                .wpm(entry.getWpm())
                .accuracy(entry.getAccuracy())
                .wordsJson(entry.getWordsJson())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
