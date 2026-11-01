package com.example.readerai.service;

import com.example.readerai.dto.reading.BookParagraphsDTO;
import com.example.readerai.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

// Синхронні виклики Python-сервісу (service-to-service, той самий внутрішній
// API-ключ, що й у зворотному напрямку). Довгі задачі (генерація тестів) йдуть
// через RabbitMQ, тут — лише швидкі запити, на які чекає вчитель.
@Slf4j
@Service
public class PythonClient {

    private final RestClient restClient;

    private final String internalApiKey;

    public PythonClient(@Value("${python.base-url}") String baseUrl, @Value("${internal.api-key}") String internalApiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    // Чистий текст сторінок startPage..endPage (з 1, включно), розбитий на абзаци.
    public List<String> getBookParagraphs(String fileName, int startPage, int endPage) {
        try {
            BookParagraphsDTO response = restClient.post()
                    .uri("/internal/book-paragraphs")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("fileName", fileName, "startPage", startPage, "endPage", endPage))
                    .retrieve()
                    .body(BookParagraphsDTO.class);
            return response == null || response.getParagraphs() == null ? List.of() : response.getParagraphs();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException("Invalid page range");
        } catch (RestClientException e) {
            log.error("Failed to get paragraphs of {} pages {}-{} from Python: {}", fileName, startPage, endPage, e.getMessage());
            throw new ServiceUnavailableException("Text service is unavailable", e);
        }
    }
}
