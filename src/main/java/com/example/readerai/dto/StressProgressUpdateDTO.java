package com.example.readerai.dto;

import com.example.readerai.entity.StressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Проміжне оновлення від Python під час обробки шару перевірки наголосу
// (forced alignment -> extraction -> scoring) — окремий internal-виклик від
// фінального результату, щоб мобілка могла показати прогрес-бар.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StressProgressUpdateDTO {

    private StressStatus status;

    // 0..100
    private Integer progress;
}
