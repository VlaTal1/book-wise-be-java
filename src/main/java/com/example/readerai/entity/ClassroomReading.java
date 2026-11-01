package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// Фрагмент книги для читання на швидкість, призначений класу. Кожна спроба
// учня — звичайний ReadingSpeedAttempt з посиланням на це завдання.
@Entity
@Table(name = "CLASSROOM_READING")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReading extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CLASSROOM_READING_SEQ_ID")
    @SequenceGenerator(name = "CLASSROOM_READING_SEQ_ID", sequenceName = "CLASSROOM_READING_SEQ_ID", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASSROOM_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_READING_CLASSROOM"))
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOK_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_CLASSROOM_READING_BOOK"))
    private Book book;

    @Column(name = "START_PAGE", nullable = false)
    private Integer startPage;

    @Column(name = "END_PAGE", nullable = false)
    private Integer endPage;

    // Чистий текст обраних абзаців, абзаци розділені "\n".
    @Column(name = "TEXT", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "WORD_COUNT", nullable = false)
    private Integer wordCount;

    // null — без ліміту часу.
    @Column(name = "TIME_LIMIT_SECONDS")
    private Integer timeLimitSeconds;

    // null — без обмеження кількості спроб.
    @Column(name = "MAX_ATTEMPTS")
    private Integer maxAttempts;
}
