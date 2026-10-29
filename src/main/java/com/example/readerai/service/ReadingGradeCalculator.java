package com.example.readerai.service;

import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.entity.StressStatus;

// Оцінка читання на швидкість за 10-бальною шкалою. Кожен показник
// зводиться до 0..1 і входить зі своєю вагою; показники, яких немає (тесту не
// було, наголос не вдалося перевірити), просто випадають, а ваги решти
// перенормовуються.
public final class ReadingGradeCalculator {

    static final double SPEED_WEIGHT = 0.3;
    static final double ACCURACY_WEIGHT = 0.3;
    static final double COMPREHENSION_WEIGHT = 0.3;
    static final double STRESS_WEIGHT = 0.1;

    // Точність розпізнаних слів зазвичай 90-98%, тож шкалу починаємо з 80%,
    // інакше всі отримували б майже максимум.
    static final double ACCURACY_ZERO = 0.80;
    static final double ACCURACY_FULL = 1.00;

    // Алгоритм перевірки наголосу дає 75-90% навіть на правильному читанні —
    // калібруємо, щоб його шум не валив оцінку.
    static final double STRESS_ZERO = 0.70;
    static final double STRESS_FULL = 0.95;

    static final int DEFAULT_TARGET_WPM = 70;

    private ReadingGradeCalculator() {
    }

    public static int calculate(ReadingSpeedAttempt attempt) {
        double weightedSum = 0;
        double totalWeight = 0;

        // Темп рахуємо за правильно прочитаними словами (WCPM): швидке читання
        // з помилками не повинно давати високий бал за швидкість.
        int targetWpm = attempt.getTargetWpm() != null && attempt.getTargetWpm() > 0
                ? attempt.getTargetWpm()
                : DEFAULT_TARGET_WPM;
        double correctWordsPerMinute = attempt.getWpm() * attempt.getAccuracy();
        weightedSum += SPEED_WEIGHT * Math.min(1.0, correctWordsPerMinute / targetWpm);
        totalWeight += SPEED_WEIGHT;

        weightedSum += ACCURACY_WEIGHT * scale(attempt.getAccuracy(), ACCURACY_ZERO, ACCURACY_FULL);
        totalWeight += ACCURACY_WEIGHT;

        if (attempt.getQuizTotal() != null && attempt.getQuizTotal() > 0 && attempt.getQuizCorrect() != null) {
            weightedSum += COMPREHENSION_WEIGHT * ((double) attempt.getQuizCorrect() / attempt.getQuizTotal());
            totalWeight += COMPREHENSION_WEIGHT;
        }

        if (attempt.getStressStatus() == StressStatus.DONE && attempt.getStressAccuracy() != null) {
            weightedSum += STRESS_WEIGHT * scale(attempt.getStressAccuracy(), STRESS_ZERO, STRESS_FULL);
            totalWeight += STRESS_WEIGHT;
        }

        int grade = (int) Math.round(10 * weightedSum / totalWeight);
        return Math.max(1, Math.min(10, grade));
    }

    // Оцінка остаточна, коли перевірка наголосу завершилась (будь-чим) і
    // відомий результат тесту (або тесту не було).
    public static boolean isReady(ReadingSpeedAttempt attempt) {
        boolean stressFinished = attempt.getStressStatus() == StressStatus.DONE
                || attempt.getStressStatus() == StressStatus.FAILED
                || attempt.getStressStatus() == StressStatus.NOT_APPLICABLE;
        boolean quizFinished = attempt.getQuizTotal() == null
                || attempt.getQuizTotal() == 0
                || attempt.getQuizCorrect() != null;
        return stressFinished && quizFinished;
    }

    private static double scale(double value, double zero, double full) {
        return Math.max(0, Math.min(1, (value - zero) / (full - zero)));
    }
}
