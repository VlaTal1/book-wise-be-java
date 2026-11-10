package com.example.readerai.dto.reading;

import com.example.readerai.entity.ReadingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReadingRequest {

    // FIXED (за замовчуванням) — фрагмент за сторінками й абзацами;
    // RANDOM — випадкові уривки з книги, сторінки й абзаци не потрібні.
    private ReadingMode mode;

    // Необов'язкова назва завдання (до 100 символів).
    private String name;

    private Long bookId;

    private Integer startPage;

    private Integer endPage;

    // Індекси першого й останнього абзацу (включно) у попередньому перегляді
    // сторінок startPage..endPage.
    private Integer startParagraph;

    private Integer endParagraph;

    // null — без ліміту часу.
    private Integer timeLimitSeconds;

    // null — без обмеження кількості спроб.
    private Integer maxAttempts;
}
