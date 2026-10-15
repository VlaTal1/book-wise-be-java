package com.example.readerai.dto.statistics;

import com.example.readerai.dto.ParticipantDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSpeedStatsByParticipant {

    private ParticipantDTO participant;

    private int attemptsCount;

    private float averageWpm;

    private float averageAccuracy;

    // Null, якщо жодна спроба ще не має завершеного (DONE) результату шару
    // перевірки наголосу — окрема, асинхронна метрика, рахується не завжди
    // одночасно з wpm/accuracy (див. services/stress_background.py, Python).
    private Float averageStressAccuracy;

    private int stressCheckedAttemptsCount;
}
