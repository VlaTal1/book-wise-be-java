package com.example.readerai.dto.classroom;

import com.example.readerai.dto.AuditDTO;
import com.example.readerai.entity.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// Зведення по тесту класу для вчителя: стан генерації та скільки учнів пройшли.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomTestDTO extends AuditDTO {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private Integer startPage;

    private Integer endPage;

    private Integer questionsAmount;

    private GenerationStatus generationStatus;

    private Integer testsCount;

    private Integer completedCount;
}
