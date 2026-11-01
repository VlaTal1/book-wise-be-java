package com.example.readerai.service;

import com.example.readerai.dto.reading.ClassroomReadingDTO;
import com.example.readerai.dto.reading.ClassroomReadingRequest;
import com.example.readerai.dto.reading.ClassroomReadingResultsDTO;
import com.example.readerai.dto.reading.ReadingSessionDTO;
import com.example.readerai.dto.reading.StudentReadingResultDTO;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Classroom;
import com.example.readerai.entity.ClassroomReading;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.exception.NoAttemptsLeftException;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ClassroomBookRepository;
import com.example.readerai.repository.ClassroomReadingRepository;
import com.example.readerai.repository.ClassroomRepository;
import com.example.readerai.repository.ParticipantRepository;
import com.example.readerai.repository.ReadingSpeedAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Фрагмент книги для читання на швидкість, призначений класу. Вчитель обирає
 * сторінки, бачить їх чистий текст абзацами (Python, services/pdf_text) і
 * позначає перший та останній абзац; ліміт часу й кількість спроб — за
 * бажанням (null — без обмежень). Спроби учнів — звичайні ReadingSpeedAttempt
 * з посиланням на завдання; рахуються лише збережені (завершені) спроби.
 */
@Service
@RequiredArgsConstructor
public class ClassroomReadingService {

    // Grammar Vosk — увесь фрагмент однією фразою; дуже довгий текст і не
    // прочитати за урок, і граматика стає важкою.
    private static final int MAX_WORDS = 1500;
    private static final int MIN_TIME_LIMIT_SECONDS = 10;
    private static final int MAX_TIME_LIMIT_SECONDS = 60 * 60;
    private static final int MAX_ATTEMPTS = 100;
    private static final int PREVIEW_LENGTH = 120;

    private final ClassroomReadingRepository classroomReadingRepository;

    private final ClassroomRepository classroomRepository;

    private final ClassroomBookRepository classroomBookRepository;

    private final ParticipantRepository participantRepository;

    private final ReadingSpeedAttemptRepository readingSpeedAttemptRepository;

    private final ParticipantService participantService;

    private final BookService bookService;

    private final PythonClient pythonClient;

    // Абзаци обраних сторінок — вчитель обирає з них початок і кінець фрагмента.
    public List<String> preview(Long classroomId, Long bookId, Integer startPage, Integer endPage) {
        findOwnedClassroom(classroomId);
        Book book = findClassroomBook(classroomId, bookId);
        validatePages(startPage, endPage);
        return pythonClient.getBookParagraphs(book.getFileName(), startPage, endPage);
    }

    // Абзаци беремо заново на сервері, а не з запиту — у завданні завжди той
    // самий чистий текст, що бачив вчитель у попередньому перегляді.
    public ClassroomReadingDTO create(Long classroomId, ClassroomReadingRequest request) {
        Classroom classroom = findOwnedClassroom(classroomId);
        if (request.getBookId() == null) {
            throw new IllegalArgumentException("Book is required");
        }
        Book book = findClassroomBook(classroomId, request.getBookId());
        validatePages(request.getStartPage(), request.getEndPage());
        validateLimits(request.getTimeLimitSeconds(), request.getMaxAttempts());

        List<String> paragraphs = pythonClient.getBookParagraphs(book.getFileName(), request.getStartPage(), request.getEndPage());
        Integer start = request.getStartParagraph();
        Integer end = request.getEndParagraph();
        if (start == null || end == null || start < 0 || end < start || end >= paragraphs.size()) {
            throw new IllegalArgumentException("Invalid paragraph range");
        }

        String text = String.join("\n", paragraphs.subList(start, end + 1));
        int wordCount = countWords(text);
        if (wordCount == 0) {
            throw new IllegalArgumentException("Selected fragment has no text");
        }
        if (wordCount > MAX_WORDS) {
            throw new IllegalArgumentException("Selected fragment is too long (max " + MAX_WORDS + " words)");
        }

        ClassroomReading reading = classroomReadingRepository.save(ClassroomReading.builder()
                .classroom(classroom)
                .book(book)
                .startPage(request.getStartPage())
                .endPage(request.getEndPage())
                .text(text)
                .wordCount(wordCount)
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .maxAttempts(request.getMaxAttempts())
                .build());
        return toTeacherDTO(reading);
    }

    // Для сторінки класу у вчителя.
    public List<ClassroomReadingDTO> getForTeacher(Long classroomId) {
        return classroomReadingRepository.findByClassroom_IdOrderByCreatedAtDesc(classroomId).stream()
                .map(this::toTeacherDTO)
                .toList();
    }

    // Завдання класу поточного учня — з його спробами та найкращою оцінкою.
    public List<ClassroomReadingDTO> getForCurrentStudent() {
        Participant student = participantService.requireStudent();
        if (student.getClassroom() == null) {
            return List.of();
        }
        return classroomReadingRepository.findByClassroom_IdOrderByCreatedAtDesc(student.getClassroom().getId()).stream()
                .map(reading -> {
                    List<ReadingSpeedAttempt> attempts = readingSpeedAttemptRepository
                            .findAllByClassroomReading_IdAndParticipant_Id(reading.getId(), student.getId());
                    ClassroomReadingDTO dto = toBaseDTO(reading);
                    dto.setAttemptsUsed(attempts.size());
                    dto.setBestGrade(bestAttempt(attempts).map(ReadingSpeedAttempt::getGrade).orElse(null));
                    return dto;
                })
                .toList();
    }

    public ClassroomReadingResultsDTO getResults(Long classroomId, Long readingId) {
        ClassroomReading reading = findOwnedReading(classroomId, readingId);

        List<StudentReadingResultDTO> students = participantRepository.findByClassroom_IdOrderByName(classroomId).stream()
                .map(student -> {
                    List<ReadingSpeedAttempt> attempts = readingSpeedAttemptRepository
                            .findAllByClassroomReading_IdAndParticipant_Id(readingId, student.getId());
                    Optional<ReadingSpeedAttempt> best = bestAttempt(attempts);
                    return StudentReadingResultDTO.builder()
                            .participantId(student.getId())
                            .participantName(student.getName())
                            .attemptsCount(attempts.size())
                            .bestGrade(best.map(ReadingSpeedAttempt::getGrade).orElse(null))
                            .bestAttemptId(best.map(ReadingSpeedAttempt::getId).orElse(null))
                            .bestWpm(best.map(ReadingSpeedAttempt::getWpm).orElse(null))
                            .gradePending(attempts.stream().anyMatch(attempt -> attempt.getGrade() == null))
                            .build();
                })
                // Від найкращої оцінки до найгіршої; без оцінки — у кінці
                // (спершу ті, хто вже читав, потім ті, хто ще ні).
                .sorted(Comparator
                        .comparing(StudentReadingResultDTO::getBestGrade, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StudentReadingResultDTO::getAttemptsCount, Comparator.reverseOrder())
                        .thenComparing(StudentReadingResultDTO::getParticipantName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return ClassroomReadingResultsDTO.builder()
                .reading(toTeacherDTO(reading))
                .students(students)
                .build();
    }

    // Спроби учнів лишаються в їхній історії, лише відв'язуються від завдання.
    @Transactional
    public void delete(Long classroomId, Long readingId) {
        detachAndDelete(findOwnedReading(classroomId, readingId));
    }

    // При видаленні класу (ClassroomService.delete).
    @Transactional
    public void deleteAllForClassroom(Long classroomId) {
        classroomReadingRepository.findByClassroom_IdOrderByCreatedAtDesc(classroomId).forEach(this::detachAndDelete);
    }

    // Python на старті сесії: чи може учень читати цей фрагмент і що саме.
    public ReadingSessionDTO getSession(Long readingId, Long participantId) {
        ClassroomReading reading = classroomReadingRepository.findById(readingId)
                .orElseThrow(() -> new NotFoundException("Reading assignment not found"));
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));
        if (participant.getClassroom() == null
                || !Objects.equals(participant.getClassroom().getId(), reading.getClassroom().getId())) {
            throw new PermissionDeniedException("Participant is not in this classroom");
        }

        int attemptsUsed = (int) readingSpeedAttemptRepository.countByClassroomReading_IdAndParticipant_Id(readingId, participantId);
        if (reading.getMaxAttempts() != null && attemptsUsed >= reading.getMaxAttempts()) {
            throw new NoAttemptsLeftException("No attempts left");
        }

        return ReadingSessionDTO.builder()
                .id(reading.getId())
                .title(reading.getBook().getTitle())
                .text(reading.getText())
                .timeLimitSeconds(reading.getTimeLimitSeconds())
                .maxAttempts(reading.getMaxAttempts())
                .attemptsUsed(attemptsUsed)
                .build();
    }

    public Optional<ClassroomReading> findById(Long id) {
        return id == null ? Optional.empty() : classroomReadingRepository.findById(id);
    }

    private void detachAndDelete(ClassroomReading reading) {
        for (ReadingSpeedAttempt attempt : readingSpeedAttemptRepository.findAllByClassroomReading_Id(reading.getId())) {
            attempt.setClassroomReading(null);
            readingSpeedAttemptRepository.save(attempt);
        }
        classroomReadingRepository.delete(reading);
    }

    // Найкраща оцінена спроба; за рівної оцінки — вищий темп.
    private static Optional<ReadingSpeedAttempt> bestAttempt(List<ReadingSpeedAttempt> attempts) {
        return attempts.stream()
                .filter(attempt -> attempt.getGrade() != null)
                .max(Comparator.comparing(ReadingSpeedAttempt::getGrade)
                        .thenComparing(ReadingSpeedAttempt::getWpm, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private ClassroomReadingDTO toTeacherDTO(ClassroomReading reading) {
        ClassroomReadingDTO dto = toBaseDTO(reading);
        List<Participant> students = participantRepository.findByClassroom_IdOrderByName(reading.getClassroom().getId());
        List<ReadingSpeedAttempt> attempts = readingSpeedAttemptRepository.findAllByClassroomReading_Id(reading.getId());
        dto.setStudentsCount(students.size());
        dto.setCompletedCount((int) students.stream()
                .filter(student -> attempts.stream().anyMatch(attempt -> Objects.equals(attempt.getParticipant().getId(), student.getId())))
                .count());
        return dto;
    }

    private ClassroomReadingDTO toBaseDTO(ClassroomReading reading) {
        String text = reading.getText().replace('\n', ' ');
        return ClassroomReadingDTO.builder()
                .id(reading.getId())
                .classroomId(reading.getClassroom().getId())
                .bookId(reading.getBook().getId())
                .bookTitle(reading.getBook().getTitle())
                .startPage(reading.getStartPage())
                .endPage(reading.getEndPage())
                .wordCount(reading.getWordCount())
                .preview(text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH).trim() + "…" : text)
                .timeLimitSeconds(reading.getTimeLimitSeconds())
                .maxAttempts(reading.getMaxAttempts())
                .createdAt(reading.getCreatedAt())
                .updatedAt(reading.getUpdatedAt())
                .build();
    }

    private Classroom findOwnedClassroom(Long classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom with id " + classroomId + " not found"));
        Participant teacher = participantService.requireTeacher();
        if (!Objects.equals(classroom.getTeacherUserId(), teacher.getUserId())) {
            throw new PermissionDeniedException("You are not the teacher of this classroom");
        }
        return classroom;
    }

    private ClassroomReading findOwnedReading(Long classroomId, Long readingId) {
        findOwnedClassroom(classroomId);
        ClassroomReading reading = classroomReadingRepository.findById(readingId)
                .orElseThrow(() -> new NotFoundException("Reading assignment not found"));
        if (!Objects.equals(reading.getClassroom().getId(), classroomId)) {
            throw new NotFoundException("Reading assignment not found");
        }
        return reading;
    }

    // Як і тести, фрагмент можна взяти лише з книги, призначеної класу.
    private Book findClassroomBook(Long classroomId, Long bookId) {
        if (bookId == null || !classroomBookRepository.existsByClassroom_IdAndBook_Id(classroomId, bookId)) {
            throw new IllegalArgumentException("Book is not assigned to this classroom");
        }
        Book book = bookService.findOwnedBook(bookId);
        if (!"application/pdf".equals(book.getFileType())) {
            throw new IllegalArgumentException("Only PDF books are supported for reading assignments");
        }
        return book;
    }

    private static void validatePages(Integer startPage, Integer endPage) {
        if (startPage == null || endPage == null || startPage < 1 || endPage < startPage) {
            throw new IllegalArgumentException("Invalid page range");
        }
    }

    private static void validateLimits(Integer timeLimitSeconds, Integer maxAttempts) {
        if (timeLimitSeconds != null && (timeLimitSeconds < MIN_TIME_LIMIT_SECONDS || timeLimitSeconds > MAX_TIME_LIMIT_SECONDS)) {
            throw new IllegalArgumentException("Time limit must be between " + MIN_TIME_LIMIT_SECONDS + " and " + MAX_TIME_LIMIT_SECONDS + " seconds");
        }
        if (maxAttempts != null && (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS)) {
            throw new IllegalArgumentException("Attempts must be between 1 and " + MAX_ATTEMPTS);
        }
    }

    private static int countWords(String text) {
        return (int) java.util.regex.Pattern.compile("[\\p{L}\\p{N}]+").matcher(text).results().count();
    }
}
