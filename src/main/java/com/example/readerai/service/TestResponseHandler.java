package com.example.readerai.service;

import com.example.readerai.dto.rabbit.TestGenerationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestResponseHandler {

    private final TestService testService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTestGenerationResponse(Map<String, Object> responseMap) {
        try {
            log.info("Received response from test generation service: {}", responseMap);

            TestGenerationResponse response = objectMapper.convertValue(responseMap, TestGenerationResponse.class);

            try {
                testService.saveTestResult(response);
            } catch (Exception e) {
                log.error("Test generation failed for test {}: {}", response.getTestId(), e.getMessage());
                testService.markGenerationFailed(response.getTestId());
            }
        } catch (Exception e) {
            log.error("Failed while processing test generation response: {}", e.getMessage(), e);
        }
    }
}