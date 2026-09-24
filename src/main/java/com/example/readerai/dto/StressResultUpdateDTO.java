package com.example.readerai.dto;

import com.example.readerai.entity.StressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Фінальний результат шару перевірки наголосу від Python — приходить
// окремим internal-викликом, значно пізніше за сам ReadingSpeedAttempt
// (потрібен повний forced alignment на аудіо сесії).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StressResultUpdateDTO {

    private StressStatus status;

    private Double accuracy;

    private Integer checkedWords;

    private Integer correctWords;

    // Пословний вердикт як сирий JSON-масив (Java лише зберігає і повертає,
    // не парсячи структуру — той самий підхід, що і wordsJson).
    private String wordsJson;

    // Повідомлення про помилку, якщо status == FAILED (наприклад, MFA
    // недоступний або forced alignment не зміг вирівняти аудіо).
    private String error;
}
