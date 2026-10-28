package com.example.readerai.controller;

import com.example.readerai.dto.BookDTO;
import com.example.readerai.dto.BookWithDetails;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.service.BookService;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        List<BookDTO> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/participant/{participantId}")
    public ResponseEntity<List<BookDTO>> getAllBooksByParticipantId(@PathVariable Long participantId) {
        List<BookDTO> books = bookService.getVisibleBooksByParticipantId(participantId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookWithDetails> getBookById(@PathVariable Long id) throws NotFoundException {
        BookWithDetails book = bookService.getBookWithDetails(id);
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<BookDTO> uploadBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("file") MultipartFile file) throws IOException, MinioException {

        BookDTO bookDTO = bookService.uploadBook(title, author, file);
        return ResponseEntity.ok(bookDTO);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadBook(@PathVariable Long id) throws MinioException, NotFoundException {
        BookDTO book = bookService.getBookById(id);
        InputStream content = bookService.getBookContent(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + book.getOriginalFileName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(book.getFileType()))
                .body(new InputStreamResource(content));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author) {

        BookDTO bookDTO = bookService.updateBook(id, title, author);
        return ResponseEntity.ok(bookDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) throws MinioException {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
