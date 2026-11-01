package com.example.readerai.dto.reading;

import com.example.readerai.entity.ReadingMode;
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

    private ReadingMode mode;

    private String title;

    // FIXED — готовий текст фрагмента.
    private String text;

    // RANDOM — книга, з якої Python обирає випадковий уривок (для книг
    // каталогу — з підготовлених уривків за catalogKey).
    private Long bookId;

    private String fileName;

    private String fileType;

    private String catalogKey;

    private Integer timeLimitSeconds;

    private Integer maxAttempts;

    private Integer attemptsUsed;
}
