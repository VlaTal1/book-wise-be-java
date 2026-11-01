package com.example.readerai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO extends AuditDTO {

    private Long id;

    private String title;

    private String author;

    private String userId;

    private String filePath;

    private String fileName;

    private String originalFileName;

    private Long fileSize;

    private String fileType;

    private String catalogKey;
}
