package com.example.readerai.dto.reading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Рядок результатів завдання: найкраща оцінка учня і скільки спроб використано.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReadingResultDTO {

    private Long participantId;

    private String participantName;

    private Integer attemptsCount;

    // null — ще жодної оціненої спроби.
    private Integer bestGrade;

    // Спроба з найкращою оцінкою — для переходу до деталей.
    private Long bestAttemptId;

    private Double bestWpm;

    // Складові оцінки найкращої спроби (по 0-5) і сирі показники, з яких
    // вони пораховані — для картки учня в результатах.
    private Integer bestSpeedScore;

    private Integer bestPronunciationScore;

    // null — у спробі не було тесту на розуміння.
    private Integer bestComprehensionScore;

    private Double bestAccuracy;

    private Integer bestQuizTotal;

    private Integer bestQuizCorrect;

    // Є спроби, оцінка яких ще рахується (наголос / тест на розуміння).
    private Boolean gradePending;
}
