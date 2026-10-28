package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "CLASSROOM")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Classroom extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CLASSROOM_SEQ_ID")
    @SequenceGenerator(name = "CLASSROOM_SEQ_ID", sequenceName = "CLASSROOM_SEQ_ID", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    // Код, за яким учні вступають до класу.
    @Column(name = "CODE", nullable = false, unique = true)
    private String code;

    // Supabase user id вчителя-власника класу.
    @Column(name = "TEACHER_USER_ID", nullable = false)
    private String teacherUserId;
}
