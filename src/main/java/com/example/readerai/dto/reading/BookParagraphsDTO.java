package com.example.readerai.dto.reading;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Абзаци чистого тексту сторінок (відповідь Python /internal/book-paragraphs).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookParagraphsDTO {

    private List<String> paragraphs;
}
