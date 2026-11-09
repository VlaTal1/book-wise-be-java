package com.example.readerai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.entity.StressStatus;
import org.junit.jupiter.api.Test;

class ReadingGradeCalculatorTest {

    private static ReadingSpeedAttempt attempt(double wpm, int target, double accuracy,
                                               StressStatus stressStatus, Double stress,
                                               Integer quizTotal, Integer quizCorrect) {
        ReadingSpeedAttempt attempt = new ReadingSpeedAttempt();
        attempt.setWpm(wpm);
        attempt.setTargetWpm(target);
        attempt.setAccuracy(accuracy);
        attempt.setStressStatus(stressStatus);
        attempt.setStressAccuracy(stress);
        attempt.setQuizTotal(quizTotal);
        attempt.setQuizCorrect(quizCorrect);
        return attempt;
    }

    @Test
    void perfectReadingGetsFifteen() {
        var result = ReadingGradeCalculator.calculate(attempt(130, 120, 1.0, StressStatus.DONE, 0.95, 3, 3));
        assertEquals(5, result.speed());
        assertEquals(5, result.pronunciation());
        assertEquals(5, result.comprehension());
        assertEquals(15, result.total());
    }

    @Test
    void componentsAreSummed() {
        // темп 60/120 -> 2.5 -> 3; слова 90% -> 0.75, наголос 85% -> 0.6:
        // 0.7*0.75 + 0.3*0.6 = 0.705 -> 3.5 -> 4; тест 1/3 -> 1.67 -> 2
        var result = ReadingGradeCalculator.calculate(attempt(60, 120, 0.90, StressStatus.DONE, 0.85, 3, 1));
        assertEquals(3, result.speed());
        assertEquals(4, result.pronunciation());
        assertEquals(2, result.comprehension());
        assertEquals(9, result.total());
    }

    @Test
    void failedStressCheckUsesWordsOnly() {
        // слова 80% -> 0.5 -> 2.5 -> 3
        var result = ReadingGradeCalculator.calculate(attempt(120, 120, 0.80, StressStatus.FAILED, null, 3, 3));
        assertEquals(3, result.pronunciation());
        assertEquals(13, result.total());
    }

    @Test
    void withoutQuizSumIsScaledToFifteen() {
        // (5 + 4) * 1.5 = 13.5 -> 14
        var result = ReadingGradeCalculator.calculate(attempt(120, 120, 0.92, StressStatus.DONE, 0.90, 0, null));
        assertNull(result.comprehension());
        assertEquals(5, result.speed());
        assertEquals(4, result.pronunciation());
        assertEquals(14, result.total());
    }

    @Test
    void verySlowInaccurateReadingCanGetZero() {
        var result = ReadingGradeCalculator.calculate(attempt(5, 120, 0.40, StressStatus.DONE, 0.50, 3, 0));
        assertEquals(0, result.total());
    }
}
