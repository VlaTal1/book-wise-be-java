package com.example.readerai.dto.rabbit;

import com.example.readerai.dto.QuestionDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestGenerationResponse {

    private String fileName;

    private Long testId;

    private List<QuestionDTO> questions;

    // Python-сервіс заповнює замість питань, якщо генерація не вдалася.
    private String error;
}
