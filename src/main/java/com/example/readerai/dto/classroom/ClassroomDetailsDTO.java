package com.example.readerai.dto.classroom;

import com.example.readerai.dto.BookDTO;
import com.example.readerai.dto.ParticipantDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomDetailsDTO extends ClassroomDTO {

    private List<ParticipantDTO> students;

    private List<BookDTO> books;

    // Лише для вчителя класу; учень бачить свої тести на екрані тестів.
    private List<ClassroomTestDTO> tests;
}
