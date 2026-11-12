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
    void perfectReadingGetsTwelve() {
        var result = ReadingGradeCalculator.calculate(attempt(130, 120, 1.0, StressStatus.DONE, 0.95, 3, 3));
        assertEquals(4, result.speed());
        assertEquals(4, result.pronunciation());
        assertEquals(4, result.comprehension());
        assertEquals(12, result.total());
    }

    @Test
    void componentsAreSummed() {
        // темп 60/120 = 0.5 -> 2; слова 90% -> 0.75, наголос 85% -> 0.6:
        // 0.7*0.75 + 0.3*0.6 = 0.705 -> 2.82 -> 3; тест 1/3 -> 1.33 -> 1
        var result = ReadingGradeCalculator.calculate(attempt(60, 120, 0.90, StressStatus.DONE, 0.85, 3, 1));
        assertEquals(2, result.speed());
        assertEquals(3, result.pronunciation());
        assertEquals(1, result.comprehension());
        assertEquals(6, result.total());
    }

    @Test
    void failedStressCheckUsesWordsOnly() {
        // слова 80% -> 0.5 -> 2
        var result = ReadingGradeCalculator.calculate(attempt(120, 120, 0.80, StressStatus.FAILED, null, 3, 3));
        assertEquals(2, result.pronunciation());
        assertEquals(10, result.total());
    }

    @Test
    void withoutQuizSumIsScaledToTwelve() {
        // слова 92% -> 0.8, наголос 90% -> 0.8 -> 3.2 -> 3; (4 + 3) * 1.5 = 10.5 -> 11
        var result = ReadingGradeCalculator.calculate(attempt(120, 120, 0.92, StressStatus.DONE, 0.90, 0, null));
        assertNull(result.comprehension());
        assertEquals(4, result.speed());
        assertEquals(3, result.pronunciation());
        assertEquals(11, result.total());
    }

    @Test
    void verySlowInaccurateReadingCanGetZero() {
        var result = ReadingGradeCalculator.calculate(attempt(5, 120, 0.40, StressStatus.DONE, 0.50, 3, 0));
        assertEquals(0, result.total());
    }

    @Test
    void speedBands() {
        // норма 150: <37.5 -> 0, <75 -> 1, <112.5 -> 2, <150 -> 3, >=150 -> 4
        int[][] cases = {
                {0, 0}, {37, 0}, {38, 1}, {50, 1}, {74, 1},
                {75, 2}, {77, 2}, {101, 2}, {110, 2}, {112, 2},
                {113, 3}, {149, 3}, {150, 4}, {200, 4},
        };
        for (int[] c : cases) {
            var result = ReadingGradeCalculator.calculate(attempt(c[0], 150, 1.0, StressStatus.DONE, 0.95, 3, 3));
            assertEquals(c[1], result.speed(), "wpm " + c[0]);
        }
    }

    @Test
    void speedBandEdgesAreExact() {
        assertEquals(0, ReadingGradeCalculator.speedPoints(0.2499));
        assertEquals(1, ReadingGradeCalculator.speedPoints(0.25));
        assertEquals(2, ReadingGradeCalculator.speedPoints(0.5));
        assertEquals(3, ReadingGradeCalculator.speedPoints(0.75));
        assertEquals(3, ReadingGradeCalculator.speedPoints(69.0 / 70));
        assertEquals(4, ReadingGradeCalculator.speedPoints(1.0));
        assertEquals(4, ReadingGradeCalculator.speedPoints(1.7));
    }
}
