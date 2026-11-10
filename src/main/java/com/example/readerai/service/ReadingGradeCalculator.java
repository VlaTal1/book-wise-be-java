package com.example.readerai.service;

import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.entity.StressStatus;

// Оцінка читання на швидкість: три показники по 0-5 балів (цілі), підсумок —
// їхня сума, максимум 15.
//   * швидкість — темп відносно норми класу (див. speedPoints);
//   * вимова — правильність слів (70 %) і наголосу (30 %);
//   * розуміння — частка правильних відповідей тесту після читання.
// Якщо тесту не було, сума двох показників масштабується до 15.
//
// Ту саму формулу повторюють changeset-и reading-grade-15.yaml,
// reading-speed-score-bands.yaml, reading-wpm-read-words.yaml і
// reading-wpm-with-oov.yaml (перерахунок наявних оцінок) — змінюючи тут,
// додай новий changeset з перерахунком.
public final class ReadingGradeCalculator {

    public static final int COMPONENT_MAX = 5;
    public static final int GRADE_MAX = 3 * COMPONENT_MAX;

    static final double WORDS_WEIGHT = 0.7;
    static final double STRESS_WEIGHT = 0.3;

    // Точність розпізнаних слів зазвичай 90-98%, тож шкала починається не з 0,
    // інакше всі отримували б майже максимум. Але й не з 80%: на реальних
    // записях дітей з телефона кілька хибних помилок розпізнавання обнуляли
    // цю складову навіть при добрему читанні. З 60%: 90% точності = 0,75.
    static final double ACCURACY_ZERO = 0.60;
    static final double ACCURACY_FULL = 1.00;

    // Алгоритм перевірки наголосу дає 75-90% навіть на правильному читанні —
    // калібруємо, щоб його шум не валив оцінку.
    static final double STRESS_ZERO = 0.70;
    static final double STRESS_FULL = 0.95;

    static final int DEFAULT_TARGET_WPM = 70;

    // Темп нижче чверті норми — 0 балів, нижче половини — 1 бал. Від половини
    // до норми — 2-4 бали трьома рівними смугами, 5 балів — за норму і вище.
    static final double SPEED_ZERO_BELOW = 0.25;
    static final double SPEED_ONE_BELOW = 0.50;
    // Захист від похибки double на межах смуг (100/150 має дати рівно 3).
    private static final double EPS = 1e-9;

    public record Result(int speed, int pronunciation, Integer comprehension, int total) {
    }

    private ReadingGradeCalculator() {
    }

    public static Result calculate(ReadingSpeedAttempt attempt) {
        // Темп — усі слова за хвилину, крім пропущених (правильні, з помилкою
        // і відсутні у словнику Vosk), як рахує Python (alignment.py,
        // compute_metrics).
        // Помилки вже знижують бал за вимову, тож тут їх не штрафуємо вдруге.
        int targetWpm = attempt.getTargetWpm() != null && attempt.getTargetWpm() > 0
                ? attempt.getTargetWpm()
                : DEFAULT_TARGET_WPM;
        int speed = speedPoints(attempt.getWpm() / targetWpm);

        // Наголос, який не вдалося перевірити, випадає — вимова лише за словами.
        double words = scale(attempt.getAccuracy(), ACCURACY_ZERO, ACCURACY_FULL);
        double pronunciation = attempt.getStressStatus() == StressStatus.DONE && attempt.getStressAccuracy() != null
                ? WORDS_WEIGHT * words + STRESS_WEIGHT * scale(attempt.getStressAccuracy(), STRESS_ZERO, STRESS_FULL)
                : words;
        int pronunciationPoints = points(pronunciation);

        Integer comprehension = null;
        if (attempt.getQuizTotal() != null && attempt.getQuizTotal() > 0 && attempt.getQuizCorrect() != null) {
            comprehension = points((double) attempt.getQuizCorrect() / attempt.getQuizTotal());
        }

        int total = comprehension != null
                ? speed + pronunciationPoints + comprehension
                : (int) Math.round((speed + pronunciationPoints) * GRADE_MAX / (2.0 * COMPONENT_MAX));
        return new Result(speed, pronunciationPoints, comprehension, total);
    }

    public static void apply(ReadingSpeedAttempt attempt) {
        Result result = calculate(attempt);
        attempt.setSpeedScore(result.speed());
        attempt.setPronunciationScore(result.pronunciation());
        attempt.setComprehensionScore(result.comprehension());
        attempt.setGrade(result.total());
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

    //        ⎧ 0,                          r < 0.25
    //   S =  ⎨ 1,                          0.25 ≤ r < 0.5
    //        ⎩ 2 + ⌊6 · (min(r, 1) − 0.5)⌋, r ≥ 0.5
    static int speedPoints(double share) {
        if (share < SPEED_ZERO_BELOW) {
            return 0;
        }
        if (share < SPEED_ONE_BELOW) {
            return 1;
        }
        return 2 + (int) Math.floor(6 * (Math.min(share, 1.0) - SPEED_ONE_BELOW) + EPS);
    }

    private static int points(double share) {
        return (int) Math.round(COMPONENT_MAX * share);
    }

    private static double scale(double value, double zero, double full) {
        return Math.max(0, Math.min(1, (value - zero) / (full - zero)));
    }
}
