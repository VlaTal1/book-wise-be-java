package com.example.readerai.service;

import com.example.readerai.converter.BookConverter;
import com.example.readerai.dto.BookDTO;
import com.example.readerai.dto.CatalogBookDTO;
import com.example.readerai.entity.Book;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.repository.BookRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Каталог предзаготовлених книг (resources/catalog). Файли один раз
 * завантажуються в MinIO під ключем catalog/<file>; коли вчитель обирає книгу,
 * для нього створюється звичайний рядок BOOK, що посилається на спільний файл,
 * тож призначення класу, доступи учнів і генерація тестів працюють як для
 * завантажених вчителем книг.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookCatalogService {

    private static final String CATALOG_DIR = "catalog/";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final ObjectMapper objectMapper;

    private final MinioService minioService;

    private final BookRepository bookRepository;

    private final BookConverter bookConverter;

    private final UserService userService;

    private final ParticipantService participantService;

    @Value("${minio.bucketName}")
    private String bucketName;

    private List<CatalogBookDTO> catalog = List.of();

    @PostConstruct
    public void init() {
        try (InputStream json = new ClassPathResource(CATALOG_DIR + "catalog.json").getInputStream()) {
            catalog = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read book catalog", e);
        }
        for (CatalogBookDTO entry : catalog) {
            String objectName = objectName(entry);
            ClassPathResource resource = new ClassPathResource(CATALOG_DIR + entry.getFile());
            // Перезаливаємо, якщо файл у каталозі змінився (напр. очищена версія книги).
            Long size = fileSize(entry);
            if (size != null && size.equals(minioService.fileSize(objectName))) {
                continue;
            }
            try (InputStream stream = resource.getInputStream()) {
                minioService.uploadStream(objectName, stream, resource.contentLength(), PDF_CONTENT_TYPE);
                log.info("Uploaded catalog book '{}' to MinIO as {}", entry.getTitle(), objectName);
            } catch (IOException | MinioException e) {
                throw new IllegalStateException("Failed to upload catalog book " + entry.getKey(), e);
            }
        }
    }

    public List<CatalogBookDTO> getCatalog() {
        participantService.requireTeacher();
        return catalog;
    }

    @Transactional
    public BookDTO addToLibrary(String key) throws MinioException {
        participantService.requireTeacher();
        CatalogBookDTO entry = catalog.stream()
                .filter(book -> book.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Catalog book not found: " + key));

        String userId = userService.getUserId();
        Book book = bookRepository.findFirstByUserIdAndCatalogKey(userId, key)
                .orElseGet(() -> bookRepository.save(Book.builder()
                        .title(entry.getTitle())
                        .author(entry.getAuthor())
                        .userId(userId)
                        .catalogKey(entry.getKey())
                        .fileName(objectName(entry))
                        .originalFileName(entry.getFile())
                        .filePath("/" + bucketName + "/" + objectName(entry))
                        .fileSize(fileSize(entry))
                        .fileType(PDF_CONTENT_TYPE)
                        .build()));
        return bookConverter.toDTO(book);
    }

    private static String objectName(CatalogBookDTO entry) {
        return CATALOG_DIR + entry.getFile();
    }

    private static Long fileSize(CatalogBookDTO entry) {
        try {
            return new ClassPathResource(CATALOG_DIR + entry.getFile()).contentLength();
        } catch (IOException e) {
            return null;
        }
    }
}
