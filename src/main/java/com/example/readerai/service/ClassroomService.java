package com.example.readerai.service;

import com.example.readerai.converter.BookConverter;
import com.example.readerai.converter.ParticipantConverter;
import com.example.readerai.dto.CompleteStatus;
import com.example.readerai.dto.TestDTO;
import com.example.readerai.dto.classroom.ClassroomDTO;
import com.example.readerai.dto.classroom.ClassroomDetailsDTO;
import com.example.readerai.dto.classroom.ClassroomTestDTO;
import com.example.readerai.dto.classroom.ClassroomTestRequest;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Classroom;
import com.example.readerai.entity.ClassroomBook;
import com.example.readerai.entity.ClassroomTest;
import com.example.readerai.entity.GenerationStatus;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.Test;
import com.example.readerai.entity.UserRole;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ClassroomBookRepository;
import com.example.readerai.repository.ClassroomRepository;
import com.example.readerai.repository.ClassroomTestRepository;
import com.example.readerai.repository.ParticipantRepository;
import com.example.readerai.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    // Без 0/O та 1/I, щоб код легко було продиктувати й ввести з телефона.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MIN_TARGET_WPM = 10;
    private static final int MAX_TARGET_WPM = 300;

    private final ClassroomRepository classroomRepository;

    private final ClassroomBookRepository classroomBookRepository;

    private final ClassroomTestRepository classroomTestRepository;

    private final TestRepository testRepository;

    private final ParticipantRepository participantRepository;

    private final ParticipantService participantService;

    private final ParticipantConverter participantConverter;

    private final BookService bookService;

    private final BookConverter bookConverter;

    private final AccessService accessService;

    private final TestService testService;

    public ClassroomDTO create(String name, Integer targetWpm) {
        Participant teacher = participantService.requireTeacher();
        validate(name, targetWpm);

        Classroom classroom = classroomRepository.save(Classroom.builder()
                .name(name.trim())
                .code(generateUniqueCode())
                .teacherUserId(teacher.getUserId())
                .targetWpm(targetWpm)
                .build());

        return toDTO(classroom, teacher.getName());
    }

    // Нова норма впливає лише на наступні спроби читання — вже виставлені
    // оцінки зберігають норму, з якою рахувалися.
    public ClassroomDetailsDTO update(Long classroomId, String name, Integer targetWpm) {
        Classroom classroom = findOwned(classroomId);
        validate(name, targetWpm);
        classroom.setName(name.trim());
        classroom.setTargetWpm(targetWpm);
        classroomRepository.save(classroom);
        return getDetails(classroomId);
    }

    private void validate(String name, Integer targetWpm) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Classroom name cannot be empty");
        }
        if (targetWpm == null || targetWpm < MIN_TARGET_WPM || targetWpm > MAX_TARGET_WPM) {
            throw new IllegalArgumentException("Target words per minute must be between " + MIN_TARGET_WPM + " and " + MAX_TARGET_WPM);
        }
    }

    // Вчитель — усі свої класи, учень — клас, у якому він зараз (або порожньо).
    public List<ClassroomDTO> getMyClassrooms() {
        Participant current = participantService.getCurrent();
        if (current.getRole() == UserRole.TEACHER) {
            return classroomRepository.findByTeacherUserIdOrderByCreatedAtDesc(current.getUserId()).stream()
                    .map(classroom -> toDTO(classroom, current.getName()))
                    .toList();
        }
        if (current.getClassroom() == null) {
            return List.of();
        }
        return List.of(toDTO(current.getClassroom(), getTeacherName(current.getClassroom())));
    }

    public ClassroomDetailsDTO getDetails(Long classroomId) {
        Classroom classroom = findVisible(classroomId);
        ClassroomDTO base = toDTO(classroom, getTeacherName(classroom));

        return ClassroomDetailsDTO.builder()
                .id(base.getId())
                .name(base.getName())
                .code(base.getCode())
                .teacherName(base.getTeacherName())
                .studentsCount(base.getStudentsCount())
                .targetWpm(base.getTargetWpm())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .students(participantRepository.findByClassroom_IdOrderByName(classroomId).stream()
                        .map(participantConverter::toDTO)
                        .toList())
                .books(classroomBookRepository.findByClassroom_IdOrderByCreatedAtDesc(classroomId).stream()
                        .map(classroomBook -> bookConverter.toDTO(classroomBook.getBook()))
                        .toList())
                .tests(isTeacherOf(classroom)
                        ? classroomTestRepository.findByClassroom_IdOrderByCreatedAtDesc(classroomId).stream()
                                .map(this::toTestDTO)
                                .toList()
                        : List.of())
                .build();
    }

    @Transactional
    public void delete(Long classroomId) {
        Classroom classroom = findOwned(classroomId);

        for (Participant student : participantRepository.findByClassroom_IdOrderByName(classroomId)) {
            accessService.revokeAllAccesses(student);
            student.setClassroom(null);
            participantRepository.save(student);
        }
        // Тести учнів лишаються як історія, відв'язуємо їх від тесту класу.
        for (ClassroomTest classroomTest : classroomTestRepository.findByClassroom_Id(classroomId)) {
            for (Test test : testRepository.findAllByClassroomTest_Id(classroomTest.getId())) {
                test.setClassroomTest(null);
                testRepository.save(test);
            }
            classroomTestRepository.delete(classroomTest);
        }
        classroomBookRepository.deleteByClassroom_Id(classroomId);
        classroomRepository.delete(classroom);
    }

    // Учень може бути лише в одному класі — вступ до нового класу замінює
    // попередній (книги старого класу зникають зі списку, прогрес лишається).
    @Transactional
    public ClassroomDTO join(String code) {
        Participant student = participantService.requireStudent();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Classroom code cannot be empty");
        }

        Classroom classroom = classroomRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Classroom with code " + code + " not found"));

        if (student.getClassroom() != null && Objects.equals(student.getClassroom().getId(), classroom.getId())) {
            return toDTO(classroom, getTeacherName(classroom));
        }

        accessService.revokeAllAccesses(student);
        student.setClassroom(classroom);
        participantRepository.save(student);

        for (ClassroomBook classroomBook : classroomBookRepository.findByClassroom_IdOrderByCreatedAtDesc(classroom.getId())) {
            accessService.grantAccess(classroomBook.getBook(), student);
        }

        // Учень, що вступив пізніше, отримує вже призначені класу тести.
        for (ClassroomTest classroomTest : classroomTestRepository.findByClassroom_Id(classroom.getId())) {
            if (!testRepository.existsByClassroomTest_IdAndProgress_Participant_Id(classroomTest.getId(), student.getId())) {
                testService.createStudentTest(classroomTest, student);
            }
        }

        return toDTO(classroom, getTeacherName(classroom));
    }

    @Transactional
    public ClassroomDetailsDTO assignBook(Long classroomId, Long bookId) {
        Classroom classroom = findOwned(classroomId);
        Book book = bookService.findOwnedBook(bookId);

        if (!classroomBookRepository.existsByClassroom_IdAndBook_Id(classroomId, bookId)) {
            classroomBookRepository.save(ClassroomBook.builder()
                    .classroom(classroom)
                    .book(book)
                    .build());
        }

        for (Participant student : participantRepository.findByClassroom_IdOrderByName(classroomId)) {
            accessService.grantAccess(book, student);
        }

        return getDetails(classroomId);
    }

    // Один тест на клас: питання генеруються один раз, а кожен учень отримує
    // власний Test з їхньою копією. Без @Transactional навмисно: тести учнів
    // мають бути закомічені до того, як Python-сервіс поверне питання через RabbitMQ.
    public List<TestDTO> createTestForClassroom(Long classroomId, ClassroomTestRequest request) {
        Classroom classroom = findOwned(classroomId);
        if (request.getBookId() == null || request.getStartPage() == null || request.getEndPage() == null
                || request.getQuestionsAmount() == null) {
            throw new IllegalArgumentException("Book, pages and questions amount are required");
        }
        if (!classroomBookRepository.existsByClassroom_IdAndBook_Id(classroomId, request.getBookId())) {
            throw new IllegalArgumentException("Book is not assigned to this classroom");
        }

        Book book = bookService.findOwnedBook(request.getBookId());
        List<Participant> students = participantRepository.findByClassroom_IdOrderByName(classroomId);
        if (students.isEmpty()) {
            throw new IllegalArgumentException("Classroom has no students yet");
        }

        ClassroomTest classroomTest = classroomTestRepository.save(ClassroomTest.builder()
                .classroom(classroom)
                .book(book)
                .startPage(request.getStartPage())
                .endPage(request.getEndPage())
                .questionsAmount(request.getQuestionsAmount())
                .dueTo(request.getDueTo())
                .build());

        List<TestDTO> tests = students.stream()
                .map(student -> testService.createStudentTest(classroomTest, student))
                .toList();

        // testId першого учня — лише "носій" для відповіді Python-сервісу;
        // TestService.saveTestResult розкладе питання по всіх тестах класу.
        testService.requestTestGeneration(
                book.getFileName(),
                tests.get(0).getId(),
                classroomTest.getStartPage(),
                classroomTest.getEndPage(),
                classroomTest.getQuestionsAmount());

        return tests;
    }

    public ClassroomDetailsDTO regenerateTest(Long classroomId, Long classroomTestId) {
        testService.regenerate(findOwnedTest(classroomId, classroomTestId));
        return getDetails(classroomId);
    }

    // Видаляє тест класу разом з тестами учнів, їхніми відповідями й оцінками.
    @Transactional
    public ClassroomDetailsDTO deleteTest(Long classroomId, Long classroomTestId) {
        ClassroomTest classroomTest = findOwnedTest(classroomId, classroomTestId);
        testService.deleteTests(testRepository.findAllByClassroomTest_Id(classroomTestId));
        classroomTestRepository.delete(classroomTest);
        return getDetails(classroomId);
    }

    private ClassroomTest findOwnedTest(Long classroomId, Long classroomTestId) {
        findOwned(classroomId);
        ClassroomTest classroomTest = classroomTestRepository.findById(classroomTestId)
                .orElseThrow(() -> new NotFoundException("Classroom test with id " + classroomTestId + " not found"));
        if (!Objects.equals(classroomTest.getClassroom().getId(), classroomId)) {
            throw new NotFoundException("Classroom test with id " + classroomTestId + " not found");
        }
        return classroomTest;
    }

    private boolean isTeacherOf(Classroom classroom) {
        return Objects.equals(classroom.getTeacherUserId(), participantService.getCurrent().getUserId());
    }

    // Стан тесту класу зводимо зі станів тестів учнів: помилка важливіша за
    // генерацію, а "готово" — лише коли питання є в усіх.
    private ClassroomTestDTO toTestDTO(ClassroomTest classroomTest) {
        List<Test> tests = testRepository.findAllByClassroomTest_Id(classroomTest.getId());
        GenerationStatus status = GenerationStatus.READY;
        if (tests.stream().anyMatch(test -> test.getGenerationStatus() == GenerationStatus.FAILED)) {
            status = GenerationStatus.FAILED;
        } else if (tests.stream().anyMatch(test -> test.getGenerationStatus() == GenerationStatus.GENERATING)) {
            status = GenerationStatus.GENERATING;
        }

        return ClassroomTestDTO.builder()
                .id(classroomTest.getId())
                .bookId(classroomTest.getBook().getId())
                .bookTitle(classroomTest.getBook().getTitle())
                .startPage(classroomTest.getStartPage())
                .endPage(classroomTest.getEndPage())
                .questionsAmount(classroomTest.getQuestionsAmount())
                .generationStatus(status)
                .testsCount(tests.size())
                .completedCount((int) tests.stream()
                        .filter(test -> CompleteStatus.COMPLETED.toString().equals(test.getCompleted()))
                        .count())
                .createdAt(classroomTest.getCreatedAt())
                .updatedAt(classroomTest.getUpdatedAt())
                .build();
    }

    private Classroom findOwned(Long classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom with id " + classroomId + " not found"));
        Participant teacher = participantService.requireTeacher();
        if (!Objects.equals(classroom.getTeacherUserId(), teacher.getUserId())) {
            throw new PermissionDeniedException("You are not the teacher of this classroom");
        }
        return classroom;
    }

    private Classroom findVisible(Long classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom with id " + classroomId + " not found"));
        Participant current = participantService.getCurrent();
        boolean isTeacher = Objects.equals(classroom.getTeacherUserId(), current.getUserId());
        boolean isMember = current.getClassroom() != null && Objects.equals(current.getClassroom().getId(), classroomId);
        if (!isTeacher && !isMember) {
            throw new PermissionDeniedException("You do not have access to this classroom");
        }
        return classroom;
    }

    private String getTeacherName(Classroom classroom) {
        return participantRepository.findByUserId(classroom.getTeacherUserId())
                .map(Participant::getName)
                .orElse(null);
    }

    private ClassroomDTO toDTO(Classroom classroom, String teacherName) {
        return ClassroomDTO.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .code(classroom.getCode())
                .teacherName(teacherName)
                .studentsCount(participantRepository.findByClassroom_IdOrderByName(classroom.getId()).size())
                .targetWpm(classroom.getTargetWpm())
                .createdAt(classroom.getCreatedAt())
                .updatedAt(classroom.getUpdatedAt())
                .build();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (classroomRepository.existsByCode(code));
        return code;
    }
}
