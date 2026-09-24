package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

    // Окремий асинхронний шар перевірки наголосу (forced alignment + модель,
    // Python-сервіс) — рахується ПІСЛЯ того, як цей attempt вже збережено
    // (потрібен повний аудіофайл сесії), тому статус стартує з PENDING і
    // оновлюється окремими internal-викликами (не через save()).
    @Enumerated(EnumType.STRING)
    @Column(name = "STRESS_STATUS", nullable = false, length = 20)
    @Builder.Default
    private StressStatus stressStatus = StressStatus.PENDING;

    @Column(name = "STRESS_PROGRESS")
    private Integer stressProgress;

    @Column(name = "STRESS_ACCURACY")
    private Double stressAccuracy;

    @Column(name = "STRESS_CHECKED_WORDS")
    private Integer stressCheckedWords;

    @Column(name = "STRESS_CORRECT_WORDS")
    private Integer stressCorrectWords;

    // Пословний вердикт перевірки наголосу (index, word, checked, correct,
    // predicted/reference склад) як JSON-масив — той самий підхід, що і
    // wordsJson вище.
    @Column(name = "STRESS_WORDS_JSON", columnDefinition = "TEXT")
    private String stressWordsJson;

    @Column(name = "STRESS_ERROR", columnDefinition = "TEXT")
    private String stressError;
}
