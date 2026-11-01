package com.example.readerai.converter;

import com.example.readerai.dto.BookDTO;
import com.example.readerai.dto.BookWithDetails;
import com.example.readerai.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookConverter {
    public Book fromDTO(BookDTO entry) {
        return Book.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .author(entry.getAuthor())
                .userId(entry.getUserId())
                .filePath(entry.getFilePath())
                .fileName(entry.getFileName())
                .originalFileName(entry.getOriginalFileName())
                .fileSize(entry.getFileSize())
                .fileType(entry.getFileType())
                .catalogKey(entry.getCatalogKey())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    public BookDTO toDTO(Book entry) {
        return BookDTO.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .author(entry.getAuthor())
                .userId(entry.getUserId())
                .filePath(entry.getFilePath())
                .fileName(entry.getFileName())
                .originalFileName(entry.getOriginalFileName())
                .fileSize(entry.getFileSize())
                .fileType(entry.getFileType())
                .catalogKey(entry.getCatalogKey())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    public BookWithDetails fromDTOtoWithDetails(BookDTO entry) {
        return BookWithDetails.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .author(entry.getAuthor())
                .userId(entry.getUserId())
                .filePath(entry.getFilePath())
                .fileName(entry.getFileName())
                .originalFileName(entry.getOriginalFileName())
                .fileSize(entry.getFileSize())
                .fileType(entry.getFileType())
                .catalogKey(entry.getCatalogKey())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
