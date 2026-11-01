package com.example.readerai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogBookDTO {

    private String key;

    private String title;

    private String author;

    private Integer grade;

    // Не віддаємо клієнту — лише ім'я файлу в resources/catalog.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String file;
}
