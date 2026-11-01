package com.example.readerai.dto.reading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReadingResultsDTO {

    private ClassroomReadingDTO reading;

    // Від найкращої оцінки до найгіршої; учні без оцінки — у кінці.
    private List<StudentReadingResultDTO> students;
}
