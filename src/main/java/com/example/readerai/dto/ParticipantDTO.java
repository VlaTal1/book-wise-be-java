package com.example.readerai.dto;

import com.example.readerai.entity.UserRole;
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
public class ParticipantDTO extends AuditDTO {

    private Long id;

    private String supId;

    private String name;

    private String userId;

    private UserRole role;

    private Long classroomId;

    private String classroomName;
}