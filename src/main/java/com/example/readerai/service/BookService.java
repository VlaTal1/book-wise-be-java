package com.example.readerai.service;

import com.example.readerai.converter.AccessConverter;
import com.example.readerai.converter.BookConverter;
import com.example.readerai.dto.AccessDTO;
import com.example.readerai.dto.BookDTO;
import com.example.readerai.dto.BookWithDetails;
import com.example.readerai.dto.FileInfo;
import com.example.readerai.entity.Access;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.UserRole;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.AccessRepository;
import com.example.readerai.repository.BookRepository;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final MinioService minioService;

    private final UserService userService;

    private final BookRepository bookRepository;

    private final BookConverter bookConverter;

    private final AccessRepository accessRepository;

    private final AccessConverter accessConverter;

    private final ParticipantService participantService;

    // Вчитель бачить завантажені ним книги, учень — призначені його класу.
    public List<BookDTO> getAllBooks() {
        Participant current = participantService.getCurrent();
        if (current.getRole() == UserRole.STUDENT) {
            return getAllBooksByParticipantId(current.getId());
        }
        return bookRepository.findByUserId(userService.getUserId()).stream()
                .map(bookConverter::toDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> getVisibleBooksByParticipantId(Long participantId) {
        participantService.findVisible(participantId);
        return getAllBooksByParticipantId(participantId);
    }

    public List<BookDTO> getAllBooksByParticipantId(Long participantId) {
        List<Access> accesses = accessRepository.findByParticipant_Id(participantId);
        return accesses.stream().map((access -> bookConverter.toDTO(access.getBook()))).toList();
    }

    public BookDTO getBookById(Long id) {
        return bookConverter.toDTO(findVisibleBook(id));
    }

    // Книгу бачить вчитель, що її завантажив, і учні, яким її призначено.
    public Book findVisibleBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        String userId = userService.getUserId();
        if (!Objects.equals(book.getUserId(), userId) && !accessRepository.existsByBook_IdAndParticipant_UserId(id, userId)) {
            throw new PermissionDeniedException("You do not have permission to access this book");
        }
        return book;
    }

    public Book findOwnedBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        if (!Objects.equals(book.getUserId(), userService.getUserId())) {
            throw new PermissionDeniedException("Only the owner can manage this book");
        }
        return book;
    }

    public BookWithDetails getBookWithDetails(Long bookId) {
        Book entity = findVisibleBook(bookId);
        BookWithDetails bookWithDetails = bookConverter.fromDTOtoWithDetails(bookConverter.toDTO(entity));

        // Список учнів з доступом потрібен лише вчителю-власнику.
        if (!Objects.equals(entity.getUserId(), userService.getUserId())) {
            bookWithDetails.setAccesses(List.of());
            return bookWithDetails;
        }

        List<Access> accesses = accessRepository.findByBookId(bookId);
        List<AccessDTO> accessDTOS = accesses.stream()
                .map(accessConverter::toDTO)
                .toList();
        bookWithDetails.setAccesses(accessDTOS);

        return bookWithDetails;
    }

    @Transactional
    public BookDTO uploadBook(String title, String author, MultipartFile file) throws IOException, MinioException {
        participantService.requireTeacher();

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/epub+zip") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Only EPUB and PDF files are allowed");
        }

        FileInfo fileInfo = minioService.uploadFile(file);

        Book book = Book.builder()
                .title(title)
                .author(author)
                .userId(userService.getUserId())
                .fileName(fileInfo.getFileName())
                .originalFileName(fileInfo.getOriginalFileName())
                .filePath(fileInfo.getFilePath())
                .fileSize(fileInfo.getFileSize())
                .fileType(fileInfo.getFileType())
                .build();

        book = bookRepository.save(book);

        return bookConverter.toDTO(book);
    }

    public InputStream getBookContent(Long id) throws MinioException {
        Book book = findVisibleBook(id);
        return minioService.getFile(book.getFileName());
    }

    @Transactional
    public BookDTO updateBook(Long id, String title, String author) {
        Book book = findOwnedBook(id);

        book.setTitle(title);
        book.setAuthor(author);

        book = bookRepository.save(book);
        return bookConverter.toDTO(book);
    }

    @Transactional
    public void deleteBook(Long id) throws MinioException {
        Book book = findOwnedBook(id);

        minioService.deleteFile(book.getFileName());

        bookRepository.delete(book);
    }
}
