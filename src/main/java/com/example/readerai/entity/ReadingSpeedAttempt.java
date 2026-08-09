package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "READING_SPEED_ATTEMPT")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSpeedAttempt extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "READING_SPEED_ATTEMPT_SEQ_ID")
    @SequenceGenerator(name = "READING_SPEED_ATTEMPT_SEQ_ID", sequenceName = "READING_SPEED_ATTEMPT_SEQ_ID", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARTICIPANT_ID", nullable = false, referencedColumnName = "ID", foreignKey = @ForeignKey(name = "FK_READING_SPEED_ATTEMPT_PARTICIPANT"))
    private Participant participant;

    // Ідентифікатор заготовленого тексту на стороні Python-сервісу (напр. "text_1").
    // Не FK на Book — у MVP еталонний текст не привʼязаний до книги користувача,
    // див. docs/reading-speed-feature-design.md, розділ 1/5.1.
    @Column(name = "TEXT_ID", nullable = false)
    private String textId;

    @Column(name = "TOTAL_WORDS", nullable = false)
    private Integer totalWords;

    @Column(name = "CORRECT_COUNT", nullable = false)
    private Integer correctCount;

    @Column(name = "ERROR_COUNT", nullable = false)
    private Integer errorCount;

    // Включає слова зі статусом "не розпізнано" (рішення по п.4, розділ 8 дока)
    @Column(name = "SKIPPED_COUNT", nullable = false)
    private Integer skippedCount;

    // Підмножина skippedCount — слова, відсутні у словнику ASR-моделі,
    // зберігається окремо для майбутнього перерахунку батьком
    @Column(name = "NOT_IN_VOCABULARY_COUNT", nullable = false)
    private Integer notInVocabularyCount;

    @Column(name = "DURATION_SECONDS", nullable = false)
    private Double durationSeconds;

    @Column(name = "WPM", nullable = false)
    private Double wpm;

    @Column(name = "ACCURACY", nullable = false)
    private Double accuracy;

    // Пословний результат (index + status) як JSON-масив — для історії/повтору
    // перегляду підсвітки; зберігається як є, без окремих сутностей на слово.
    @Column(name = "WORDS_JSON", nullable = false, columnDefinition = "TEXT")
    private String wordsJson;
}
