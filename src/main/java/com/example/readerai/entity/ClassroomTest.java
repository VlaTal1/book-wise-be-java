package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

// Тест, призначений класу. Питання генеруються один раз, а кожен учень
// отримує власний Test (зі своїми відповідями й оцінкою) з копією цих питань.
@Entity
@Table(name = "CLASSROOM_TEST")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomTest extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CLASSROOM_TEST_SEQ_ID")
    @SequenceGenerator(name = "CLASSROOM_TEST_SEQ_ID", sequenceName = "CLASSROOM_TEST_SEQ_ID", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASSROOM_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_TEST_CLASSROOM"))
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOK_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_TEST_BOOK"))
    private Book book;

    @Column(name = "START_PAGE", nullable = false)
    private Integer startPage;

    @Column(name = "END_PAGE", nullable = false)
    private Integer endPage;

    @Column(name = "QUESTIONS_AMOUNT", nullable = false)
    private Integer questionsAmount;

    @Column(name = "DUE_TO")
    private LocalDateTime dueTo;
}
