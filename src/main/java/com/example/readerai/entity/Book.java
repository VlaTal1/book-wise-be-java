package com.example.readerai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "BOOK")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Book extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BOOK_SEQ_ID")
    @SequenceGenerator(name = "BOOK_SEQ_ID", sequenceName = "BOOK_SEQ_ID", allocationSize = 1)
    private Long id;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "AUTHOR")
    private String author;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "FILE_PATH", nullable = false)
    private String filePath;

    @Column(name = "FILE_NAME", nullable = false)
    private String fileName;

    @Column(name = "ORIGINAL_FILE_NAME")
    private String originalFileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "FILE_TYPE", nullable = false)
    private String fileType;

    // Ключ книги з вбудованого каталогу; null — книга, завантажена вчителем.
    @Column(name = "CATALOG_KEY")
    private String catalogKey;
}
