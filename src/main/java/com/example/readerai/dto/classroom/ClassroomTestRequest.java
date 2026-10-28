package com.example.readerai.dto.classroom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomTestRequest {

    private Long bookId;

    private Integer startPage;

    private Integer endPage;

    private Integer questionsAmount;

    private LocalDateTime dueTo;
}
