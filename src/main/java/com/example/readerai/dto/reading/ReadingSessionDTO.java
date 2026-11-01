package com.example.readerai.dto.reading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Для Python-сервісу на старті сесії читання призначеного фрагмента.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSessionDTO {

    private Long id;

    private String title;

    private String text;

    private Integer timeLimitSeconds;

    private Integer maxAttempts;

    private Integer attemptsUsed;
}
