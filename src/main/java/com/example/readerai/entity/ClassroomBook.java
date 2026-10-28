package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "CLASSROOM_BOOK")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomBook extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CLASSROOM_BOOK_SEQ_ID")
    @SequenceGenerator(name = "CLASSROOM_BOOK_SEQ_ID", sequenceName = "CLASSROOM_BOOK_SEQ_ID", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASSROOM_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_BOOK_CLASSROOM"))
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOK_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_BOOK_BOOK"))
    private Book book;
}
