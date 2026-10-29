package com.example.readerai.dto.classroom;

import com.example.readerai.dto.AuditDTO;
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
public class ClassroomDTO extends AuditDTO {

    private Long id;

    private String name;

    private String code;

    private String teacherName;

    private Integer studentsCount;

    private Integer targetWpm;
}
