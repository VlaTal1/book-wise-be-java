package com.example.readerai.service;

import com.example.readerai.config.RabbitMQConfig;
import com.example.readerai.converter.QuestionConverter;
import com.example.readerai.converter.TestConverter;
import com.example.readerai.dto.*;
import com.example.readerai.dto.rabbit.TestGenerationRequest;
import com.example.readerai.dto.rabbit.TestGenerationResponse;
import com.example.readerai.entity.Answer;
import com.example.readerai.entity.ClassroomTest;
import com.example.readerai.entity.GenerationStatus;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.Progress;
import com.example.readerai.entity.Question;
import com.example.readerai.entity.Test;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.AnswerRepository;
import com.example.readerai.repository.ClassroomTestRepository;
import com.example.readerai.repository.ProgressRepository;
import com.example.readerai.repository.QuestionRepository;
import com.example.readerai.repository.TestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestService {

    private final RabbitTemplate rabbitTemplate;

    private final TestRepository testRepository;

    private final TestConverter testConverter;

    private final ProgressRepository progressRepository;

    private final ClassroomTestRepository classroomTestRepository;

    private final ParticipantService participantService;

    private final QuestionRepository questionRepository;

    private final QuestionConverter questionConverter;

    private final AnswerRepository answerRepository;

    private final UserService userService;

    private final AccessService accessService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Тест учня на основі тесту класу. Генерацію тут не запускаємо — її
    // запускає ClassroomService один раз на весь клас. Якщо питання для
    // тесту класу вже згенеровані (учень вступив пізніше), копіюємо їх одразу.
    @Transactional
    public TestDTO createStudentTest(ClassroomTest classroomTest, Participant participant) {
        accessService.grantAccess(classroomTest.getBook(), participant);
        Progress progress = progressRepository.findByBookIdAndParticipantId(classroomTest.getBook().getId(), participant.getId());

        List<Test> siblings = testRepository.findAllByClassroomTest_Id(classroomTest.getId());
        boolean failed = siblings.stream().anyMatch(sibling -> sibling.getGenerationStatus() == GenerationStatus.FAILED);

        Test test = testRepository.save(Test.builder()
                .progress(progress)
                .classroomTest(classroomTest)
                .generationStatus(failed ? GenerationStatus.FAILED : GenerationStatus.GENERATING)
                .questionsAmount(classroomTest.getQuestionsAmount())
                .startPage(classroomTest.getStartPage())
                .endPage(classroomTest.getEndPage())
                .dueTo(classroomTest.getDueTo())
                .completed(CompleteStatus.NOT_STARTED.toString())
                .build());

        findGeneratedQuestions(classroomTest).ifPresent(questions -> saveQuestions(test, questions));

        return testConverter.toDTO(test);
    }

    private Optional<List<QuestionDTO>> findGeneratedQuestions(ClassroomTest classroomTest) {
        return testRepository.findAllByClassroomTest_Id(classroomTest.getId()).stream()
                .filter(test -> test.getQuestions() != null && !test.getQuestions().isEmpty())
                .findFirst()
                .map(test -> test.getQuestions().stream().map(questionConverter::toDTO).toList());
    }

    public void requestTestGeneration(String fileName, Long testId, int startPage, int endPage, int questionCount) {
        try {
            TestGenerationRequest request = new TestGenerationRequest();
            request.setFileName(fileName);
            request.setTestId(testId);
            request.setStartPage(startPage);
            request.setEndPage(endPage);
            request.setQuestionCount(questionCount);

            log.info("Sending request for test generation for test: {}", testId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    objectMapper.writeValueAsString(request)
            );
        } catch (Exception e) {
            log.error("Failed sending request for test generation for test: {}", e.getMessage(), e);
            throw new RuntimeException("Failed sending request for test generation", e);
        }
    }

    @Transactional
    public void saveTestResult(TestGenerationResponse response) {
        try {
            log.info("Saving test generation results for test: {}", response.getTestId());
            Test test = testRepository.findById(response.getTestId())
                    .orElseThrow(() -> {
                        log.error("Test with id: {} not found", response.getTestId());
                        return new NotFoundException("Test with id: " + response.getTestId() + " not found");
                    });

            if (response.getError() != null || response.getQuestions() == null || response.getQuestions().isEmpty()) {
                throw new IllegalStateException("Generation failed: " + response.getError());
            }

            // Тест класу: одна генерація — ті самі питання в тестах усіх учнів.
            for (Test target : getGenerationTargets(test)) {
                if (target.getQuestions() == null || target.getQuestions().isEmpty()) {
                    saveQuestions(target, response.getQuestions());
                }
            }

            log.info("Test generation results saved successfully for test: {}", response.getTestId());
        } catch (Exception e) {
            log.error("Failed to save test generation results for test: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save test generation results", e);
        }
    }

    // Створює нові Question/Answer для тесту (id з DTO ігноруються — кожен
    // учень отримує власні записи, щоб відповіді й вибір не перетиналися).
    private void saveQuestions(Test test, List<QuestionDTO> questionDTOs) {
        List<Question> questions = new ArrayList<>();
        for (QuestionDTO questionDTO : questionDTOs) {
            Question question = questionConverter.fromDTO(questionDTO);
            question.setId(null);
            question.setTest(test);

            List<Answer> answers = new ArrayList<>(question.getAnswers() != null ? question.getAnswers() : List.of());
            question.setAnswers(null);
            questionRepository.save(question);

            List<Answer> savedAnswers = new ArrayList<>();
            for (Answer answer : answers) {
                answer.setId(null);
                answer.setSelected(null);
                answer.setQuestion(question);
                answerRepository.save(answer);
                savedAnswers.add(answer);
            }

            question.setAnswers(savedAnswers);
            questions.add(question);
        }

        test.setQuestions(questions);
        // Python-сервіс може пропустити питання, текст для яких заблокувала
        // модель, — показуємо фактичну кількість, а не запитану.
        test.setQuestionsAmount(questions.size());
        test.setGenerationStatus(GenerationStatus.READY);
        testRepository.save(test);
        if (test.getClassroomTest() != null && !Objects.equals(test.getClassroomTest().getQuestionsAmount(), questions.size())) {
            test.getClassroomTest().setQuestionsAmount(questions.size());
            classroomTestRepository.save(test.getClassroomTest());
        }
    }

    private List<Test> getGenerationTargets(Test test) {
        return test.getClassroomTest() != null
                ? testRepository.findAllByClassroomTest_Id(test.getClassroomTest().getId())
                : List.of(test);
    }

    // Викликається, коли Python-сервіс повідомив про помилку або питання не
    // вдалося зберегти — інакше тести назавжди лишилися б у стані GENERATING.
    @Transactional
    public void markGenerationFailed(Long testId) {
        if (testId == null) {
            return;
        }
        testRepository.findById(testId).ifPresent(test -> {
            for (Test target : getGenerationTargets(test)) {
                if (target.getGenerationStatus() != GenerationStatus.READY) {
                    target.setGenerationStatus(GenerationStatus.FAILED);
                    testRepository.save(target);
                }
            }
        });
    }

    // Повторна генерація для тестів класу, яким питання ще не дісталися.
    public void regenerate(ClassroomTest classroomTest) {
        List<Test> targets = testRepository.findAllByClassroomTest_Id(classroomTest.getId()).stream()
                .filter(test -> test.getGenerationStatus() != GenerationStatus.READY)
                .toList();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Test is already generated");
        }

        for (Test target : targets) {
            target.setGenerationStatus(GenerationStatus.GENERATING);
            testRepository.save(target);
        }

        requestTestGeneration(
                classroomTest.getBook().getFileName(),
                targets.get(0).getId(),
                classroomTest.getStartPage(),
                classroomTest.getEndPage(),
                classroomTest.getQuestionsAmount());
    }

    // Видаляє тести учнів разом з питаннями, відповідями й оцінками.
    @Transactional
    public void deleteTests(List<Test> tests) {
        for (Test test : tests) {
            if (test.getQuestions() != null) {
                for (Question question : test.getQuestions()) {
                    if (question.getAnswers() != null) {
                        answerRepository.deleteAll(question.getAnswers());
                    }
                }
                questionRepository.deleteAll(test.getQuestions());
            }
            testRepository.delete(test);
        }
    }

    public List<TestDTO> getTestsByParticipantId(Long participantId) {
        participantService.findVisible(participantId);
        try {
            List<Test> tests = testRepository.findAllByProgress_Participant_Id(participantId);
            return tests.stream().map(testConverter::toDTO).toList();
        } catch (Exception e) {
            log.error("Failed to get tests by participant id: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get tests by participant id", e);
        }
    }

    public List<TestDTO> getTestsByParticipantIdAndBookId(Long participantId, Long bookId) {
        participantService.findVisible(participantId);
        try {
            List<Test> tests = testRepository.findAllByProgress_Participant_IdAndProgress_Book_Id(participantId, bookId);
            return tests.stream().map(testConverter::toDTO).toList();
        } catch (Exception e) {
            log.error("Failed to get tests by participant id and book id: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get tests by participant id and book id", e);
        }
    }

    public List<TestDTO> getTests() {
        try {
            List<Test> tests = testRepository.findAllByProgress_Participant_IdIn(participantService.getVisibleStudentIds());
            return tests.stream().map(testConverter::toDTO).toList();
        } catch (Exception e) {
            log.error("Failed to get tests: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get tests", e);
        }
    }

    public TestDTO getFirstTest(Long participantId, Long bookId) {
        participantService.findVisible(participantId);
        try {
            Test test = testRepository.getFirstTestByParticipantIdAndBookId(participantId, bookId);
            if (test == null) {
                return null;
            }
            return testConverter.toDTO(test);
        } catch (Exception e) {
            log.error("Failed to get test by participant id: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get test by participant id", e);
        }
    }

    public TestDTO savePassedTest(TestDTO testDTO) {
        int correctAnswers = 0;

        Test test = testRepository.findById(testDTO.getId()).orElseThrow(() -> new NotFoundException("Test with id " + testDTO.getId() + " not found"));

        if (!Objects.equals(test.getProgress().getParticipant().getUserId(), userService.getUserId())) {
            log.error("User do not have permission to this test");
            throw new PermissionDeniedException("User do not have permission to this test");
        }
        if (Objects.equals(test.getCompleted(), CompleteStatus.COMPLETED.toString())) {
            log.error("Test with id {} already completed", testDTO.getId());
            throw new IllegalArgumentException("Test with id " + testDTO.getId() + " already completed");
        }
        if (testDTO.getQuestions() == null || testDTO.getQuestions().isEmpty()) {
            log.error("No question provided for test with id: {}", testDTO.getId());
            throw new IllegalArgumentException("No question provided for test with id: " + testDTO.getId());
        }

        for (QuestionDTO questionDTO : testDTO.getQuestions()) {
            if (questionDTO.getAnswers() == null || questionDTO.getAnswers().isEmpty()) {
                log.error("No answer provided for question with id: {}", questionDTO.getId());
                throw new IllegalArgumentException("No answer provided for question with id: " + questionDTO.getId());
            }
            for (AnswerDTO answerDTO : questionDTO.getAnswers()) {
                if (answerDTO.getSelected() == null) {
                    log.error("Selected field in answer not nullable for save. Answer id: {}", answerDTO.getId());
                    throw new IllegalArgumentException("Selected field in answer not nullable for save. Answer id: " + answerDTO.getId());
                }
                if (answerDTO.getSelected() && answerDTO.getCorrect()) {
                    correctAnswers += 1;
                }
            }
        }

        float grade = ((float) correctAnswers / testDTO.getQuestions().size()) * 10f;

        // Оновлюємо завантажену сутність, а не зібрану з DTO клієнта — інакше
        // втратилися б зв'язок з тестом класу та статус генерації.
        test.setCompleted(CompleteStatus.COMPLETED.toString());
        test.setPassedDate(LocalDateTime.now());
        test.setGrade(Math.round(grade));
        test.setCorrectAnswers(correctAnswers);

        Test savedTest = testRepository.save(test);
        return testConverter.toDTO(savedTest);
    }
}