package com.example.readerai.dto;

import com.example.readerai.entity.StressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSpeedAttemptDTO extends AuditDTO {

    private Long id;

    private Long participantId;

    private String textId;

    private Integer totalWords;

    private Integer correctCount;

    private Integer errorCount;

    private Integer skippedCount;

    private Integer notInVocabularyCount;

    private Double durationSeconds;

    private Double wpm;

    private Double accuracy;

    // Пословний результат як сирий JSON-масив (Python вже формує його готовим,
    // Java лише зберігає і повертає, не парсячи структуру).
    private String wordsJson;

    @Builder.Default
    private StressStatus stressStatus = StressStatus.PENDING;

    private Integer stressProgress;

    private Double stressAccuracy;

    private Integer stressCheckedWords;

    private Integer stressCorrectWords;

    private String stressWordsJson;

    private String stressError;
}
