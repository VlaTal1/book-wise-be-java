package com.example.readerai.dto;

import com.example.readerai.entity.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TestDTO extends AuditDTO {

    private Long id;

    private ProgressDTO progress;

    private Integer questionsAmount;

    private Integer startPage;

    private Integer endPage;

    private CompleteStatus completed;

    private LocalDateTime dueTo;

    private LocalDateTime passedDate;

    private Integer grade;

    private Integer correctAnswers;

    private List<QuestionDTO> questions;

    private GenerationStatus generationStatus;
}