package com.example.readerai.service;

import com.example.readerai.dto.reading.ClassroomReadingRequest;
import com.example.readerai.dto.reading.ClassroomReadingResultsDTO;
import com.example.readerai.dto.reading.StudentReadingResultDTO;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Classroom;
import com.example.readerai.entity.ClassroomReading;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.ReadingMode;
import com.example.readerai.entity.ReadingSpeedAttempt;
import com.example.readerai.dto.reading.ReadingSessionDTO;
import com.example.readerai.entity.UserRole;
import com.example.readerai.repository.ClassroomBookRepository;
import com.example.readerai.repository.ClassroomReadingRepository;
import com.example.readerai.repository.ClassroomRepository;
import com.example.readerai.repository.ParticipantRepository;
import com.example.readerai.repository.ReadingSpeedAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassroomReadingServiceTest {

    private static final String TEACHER = "teacher-user";

    @Mock private ClassroomReadingRepository classroomReadingRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private ClassroomBookRepository classroomBookRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private ReadingSpeedAttemptRepository readingSpeedAttemptRepository;
    @Mock private ParticipantService participantService;
    @Mock private BookService bookService;
    @Mock private PythonClient pythonClient;

    @InjectMocks private ClassroomReadingService service;

    private Classroom classroom;
    private Book book;
    private ClassroomReading reading;

    @BeforeEach
    void setUp() {
        classroom = Classroom.builder().id(1L).name("5-А").code("ABC123").teacherUserId(TEACHER).targetWpm(70).build();
        book = Book.builder().id(2L).title("Тигролови").fileName("catalog/11_grade.pdf").fileType("application/pdf").filePath("/b").build();
        reading = ClassroomReading.builder().id(10L).classroom(classroom).book(book).startPage(3).endPage(4)
                .text("Перший абзац.\nДругий абзац.").wordCount(4).maxAttempts(3).build();

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(participantService.requireTeacher()).thenReturn(Participant.builder().userId(TEACHER).role(UserRole.TEACHER).build());
        when(classroomReadingRepository.findById(10L)).thenReturn(Optional.of(reading));
        when(classroomBookRepository.existsByClassroom_IdAndBook_Id(1L, 2L)).thenReturn(true);
        when(bookService.findOwnedBook(2L)).thenReturn(book);
        when(classroomReadingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Participant student(long id, String name) {
        return Participant.builder().id(id).name(name).role(UserRole.STUDENT).classroom(classroom).build();
    }

    private ReadingSpeedAttempt attempt(long id, Integer grade, double wpm) {
        return ReadingSpeedAttempt.builder().id(id).grade(grade).wpm(wpm).build();
    }

    @Test
    void resultsAreSortedByBestGradeAndCountAllAttempts() {
        Participant anna = student(1, "Анна");
        Participant bohdan = student(2, "Богдан");
        Participant vira = student(3, "Віра");
        Participant hlib = student(4, "Гліб");
        when(participantRepository.findByClassroom_IdOrderByName(1L)).thenReturn(List.of(anna, bohdan, vira, hlib));
        // Анна: найкраща 6 з двох спроб; Богдан: 9 і ще одна спроба рахується;
        // Віра: ще не читала; Гліб: спроба є, але оцінка ще рахується.
        when(readingSpeedAttemptRepository.findAllByClassroomReading_IdAndParticipant_Id(10L, 1L))
                .thenReturn(List.of(attempt(11, 4, 50), attempt(12, 6, 60)));
        when(readingSpeedAttemptRepository.findAllByClassroomReading_IdAndParticipant_Id(10L, 2L))
                .thenReturn(List.of(attempt(21, 9, 90), attempt(22, null, 95)));
        when(readingSpeedAttemptRepository.findAllByClassroomReading_IdAndParticipant_Id(10L, 3L)).thenReturn(List.of());
        when(readingSpeedAttemptRepository.findAllByClassroomReading_IdAndParticipant_Id(10L, 4L))
                .thenReturn(List.of(attempt(41, null, 70)));
        when(readingSpeedAttemptRepository.findAllByClassroomReading_Id(10L)).thenReturn(List.of());

        ClassroomReadingResultsDTO results = service.getResults(1L, 10L);

        assertThat(results.getStudents()).extracting(StudentReadingResultDTO::getParticipantName)
                .containsExactly("Богдан", "Анна", "Гліб", "Віра");
        StudentReadingResultDTO bohdanResult = results.getStudents().get(0);
        assertThat(bohdanResult.getBestGrade()).isEqualTo(9);
        assertThat(bohdanResult.getBestAttemptId()).isEqualTo(21L);
        assertThat(bohdanResult.getAttemptsCount()).isEqualTo(2);
        assertThat(bohdanResult.getGradePending()).isTrue();
        assertThat(results.getStudents().get(1).getBestGrade()).isEqualTo(6);
        assertThat(results.getStudents().get(2).getBestGrade()).isNull();
        assertThat(results.getStudents().get(2).getAttemptsCount()).isEqualTo(1);
        assertThat(results.getStudents().get(3).getAttemptsCount()).isZero();
    }

    @Test
    void createStoresSelectedParagraphsFromServerSideText() {
        when(pythonClient.getBookParagraphs("catalog/11_grade.pdf", 3, 4))
                .thenReturn(List.of("Нульовий абзац.", "Перший абзац тексту.", "Другий, кінцевий абзац.", "Третій."));

        ClassroomReadingRequest request = new ClassroomReadingRequest(ReadingMode.FIXED, 2L, 3, 4, 1, 2, null, 3);
        service.create(1L, request);

        ArgumentCaptor<ClassroomReading> saved = ArgumentCaptor.forClass(ClassroomReading.class);
        verify(classroomReadingRepository).save(saved.capture());
        assertThat(saved.getValue().getText()).isEqualTo("Перший абзац тексту.\nДругий, кінцевий абзац.");
        assertThat(saved.getValue().getWordCount()).isEqualTo(6);
        assertThat(saved.getValue().getTimeLimitSeconds()).isNull();
        assertThat(saved.getValue().getMaxAttempts()).isEqualTo(3);
    }

    @Test
    void createRejectsInvalidInput() {
        when(pythonClient.getBookParagraphs(any(), eq(3), eq(4))).thenReturn(List.of("Один.", "Два."));

        assertThatThrownBy(() -> service.create(1L, new ClassroomReadingRequest(ReadingMode.FIXED, 2L, 3, 4, 1, 5, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("paragraph");
        assertThatThrownBy(() -> service.create(1L, new ClassroomReadingRequest(ReadingMode.FIXED, 2L, 3, 4, 0, 1, 5, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Time limit");
        assertThatThrownBy(() -> service.create(1L, new ClassroomReadingRequest(ReadingMode.FIXED, 2L, 3, 4, 0, 1, null, 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Attempts");
        assertThatThrownBy(() -> service.create(1L, new ClassroomReadingRequest(ReadingMode.FIXED, 2L, 4, 3, 0, 1, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("page range");
        when(classroomBookRepository.existsByClassroom_IdAndBook_Id(1L, 99L)).thenReturn(false);
        assertThatThrownBy(() -> service.create(1L, new ClassroomReadingRequest(ReadingMode.FIXED, 99L, 3, 4, 0, 1, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not assigned");
        verify(classroomReadingRepository, never()).save(any());
    }

    @Test
    void deleteDetachesAttemptsInsteadOfDeletingThem() {
        ReadingSpeedAttempt kept = attempt(11, 7, 80);
        kept.setClassroomReading(reading);
        when(readingSpeedAttemptRepository.findAllByClassroomReading_Id(10L)).thenReturn(List.of(kept));

        service.delete(1L, 10L);

        assertThat(kept.getClassroomReading()).isNull();
        verify(readingSpeedAttemptRepository).save(kept);
        verify(readingSpeedAttemptRepository, never()).delete(any());
        verify(classroomReadingRepository).delete(reading);
    }

    @Test
    void randomModeNeedsNoPagesAndSessionPointsToBook() {
        ClassroomReadingRequest request = new ClassroomReadingRequest(ReadingMode.RANDOM, 2L, null, null, null, null, 120, null);
        service.create(1L, request);

        ArgumentCaptor<ClassroomReading> saved = ArgumentCaptor.forClass(ClassroomReading.class);
        verify(classroomReadingRepository).save(saved.capture());
        assertThat(saved.getValue().getMode()).isEqualTo(ReadingMode.RANDOM);
        assertThat(saved.getValue().getText()).isNull();
        assertThat(saved.getValue().getStartPage()).isNull();
        verify(pythonClient, never()).getBookParagraphs(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());

        book.setCatalogKey("11_grade");
        ClassroomReading random = ClassroomReading.builder().id(11L).classroom(classroom).book(book).mode(ReadingMode.RANDOM).timeLimitSeconds(120).build();
        when(classroomReadingRepository.findById(11L)).thenReturn(Optional.of(random));
        when(participantRepository.findById(6L)).thenReturn(Optional.of(student(6, "Макс")));
        ReadingSessionDTO session = service.getSession(11L, 6L);
        assertThat(session.getMode()).isEqualTo(ReadingMode.RANDOM);
        assertThat(session.getText()).isNull();
        assertThat(session.getFileName()).isEqualTo("catalog/11_grade.pdf");
        assertThat(session.getCatalogKey()).isEqualTo("11_grade");
        assertThat(session.getTimeLimitSeconds()).isEqualTo(120);
    }

    @Test
    void otherTeacherCannotSeeResults() {
        when(participantService.requireTeacher()).thenReturn(Participant.builder().userId("someone-else").role(UserRole.TEACHER).build());
        assertThatThrownBy(() -> service.getResults(1L, 10L))
                .isInstanceOf(com.example.readerai.exception.PermissionDeniedException.class);
        verify(readingSpeedAttemptRepository, never()).findAllByClassroomReading_IdAndParticipant_Id(anyLong(), anyLong());
    }
}
