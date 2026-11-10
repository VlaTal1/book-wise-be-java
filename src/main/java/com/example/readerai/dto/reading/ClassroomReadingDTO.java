package com.example.readerai.dto.reading;

import com.example.readerai.dto.AuditDTO;
import com.example.readerai.entity.ReadingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// Фрагмент для читання, призначений класу. Поля учня (attemptsUsed,
// bestGrade) заповнюються лише в списку завдань самого учня; поля вчителя
// (studentsCount, completedCount) — лише на сторінці класу.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReadingDTO extends AuditDTO {

    private Long id;

    private Long classroomId;

    // Назва від вчителя; null — не задана.
    private String name;

    private ReadingMode mode;

    private Long bookId;

    private String bookTitle;

    private Integer startPage;

    private Integer endPage;

    private Integer wordCount;

    // Початок тексту — щоб учень і вчитель впізнали фрагмент у списку.
    private String preview;

    private Integer timeLimitSeconds;

    private Integer maxAttempts;

    private Integer studentsCount;

    // Скільки учнів мають хоча б одну спробу.
    private Integer completedCount;

    private Integer attemptsUsed;

    private Integer bestGrade;
}
