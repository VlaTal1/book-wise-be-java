package com.example.readerai.converter;

import com.example.readerai.dto.CompleteStatus;
import com.example.readerai.dto.TestDTO;
import com.example.readerai.entity.Test;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestConverter {

    private final ProgressConverter progressConverter;

    private final QuestionConverter questionConverter;

    public Test fromDTO(TestDTO entry) {
        Test test = Test.builder()
                .id(entry.getId())
                .questionsAmount(entry.getQuestionsAmount())
                .startPage(entry.getStartPage())
                .endPage(entry.getEndPage())
                .completed(entry.getCompleted().toString())
                .dueTo(entry.getDueTo())
                .passedDate(entry.getPassedDate())
                .grade(entry.getGrade())
                .correctAnswers(entry.getCorrectAnswers())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();

        if (entry.getProgress() != null) {
            test.setProgress(progressConverter.fromDTO(entry.getProgress()));
        }

        if (entry.getQuestions() != null) {
            test.setQuestions(entry.getQuestions().stream()
                    .map(questionConverter::fromDTO)
                    .collect(Collectors.toList()));
        }

        return test;
    }

    public TestDTO toDTO(Test entry) {
        TestDTO testDTO = TestDTO.builder()
                .id(entry.getId())
                .questionsAmount(entry.getQuestionsAmount())
                .startPage(entry.getStartPage())
                .endPage(entry.getEndPage())
                .completed(CompleteStatus.valueOf(entry.getCompleted()))
                .dueTo(entry.getDueTo())
                .passedDate(entry.getPassedDate())
                .grade(entry.getGrade())
                .correctAnswers(entry.getCorrectAnswers())
                .generationStatus(entry.getGenerationStatus())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();

        if (entry.getProgress() != null) {
            testDTO.setProgress(progressConverter.toDTO(entry.getProgress()));
        }

        if (entry.getQuestions() != null) {
            testDTO.setQuestions(entry.getQuestions().stream()
                    .map(questionConverter::toDTO)
                    .collect(Collectors.toList()));
        }

        return testDTO;
    }
}