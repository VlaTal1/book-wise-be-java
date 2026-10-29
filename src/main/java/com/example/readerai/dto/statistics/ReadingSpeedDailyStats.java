package com.example.readerai.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

// На відміну від ReadingSpeedStatsByParticipant (середнє по ВСІХ дітях
// користувача одразу) — це часовий ряд ОДНІЄЇ дитини, для графіка
// "3 лінії" (швидкість/точність читання/точність наголосу по днях) на
// екрані статистики.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSpeedDailyStats {

    private LocalDate date;

    private int attemptsCount;

    private float averageWpm;

    private float averageAccuracy;

    // Null, якщо жодна спроба цього дня ще не має завершеного (DONE)
    // результату шару перевірки наголосу.
    private Float averageStressAccuracy;

    // null, якщо цього дня немає жодної оціненої спроби.
    private Float averageGrade;
}
