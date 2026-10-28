package com.example.readerai.controller;

import com.example.readerai.dto.TestDTO;
import com.example.readerai.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @GetMapping("/")
    public ResponseEntity<List<TestDTO>> getTests() {
        return ResponseEntity.ok(testService.getTests());
    }

    @GetMapping("/participant/{participantId}")
    public ResponseEntity<List<TestDTO>> getTestsByParticipantId(@PathVariable Long participantId) {
        return ResponseEntity.ok(testService.getTestsByParticipantId(participantId));
    }

    @GetMapping("/participant/{participantId}/book/{bookId}")
    public ResponseEntity<List<TestDTO>> getTestsByParticipantIdAndBookId(@PathVariable Long participantId, @PathVariable Long bookId) {
        return ResponseEntity.ok(testService.getTestsByParticipantIdAndBookId(participantId, bookId));
    }

    @GetMapping("/participant/{participantId}/book/{bookId}/firstTest")
    public ResponseEntity<TestDTO> getFirstTest(@PathVariable Long participantId, @PathVariable Long bookId) {
        return ResponseEntity.ok(testService.getFirstTest(participantId, bookId));
    }

    @PostMapping("/pass")
    public ResponseEntity<TestDTO> savePassedTest(@RequestBody TestDTO testDTO) {
        return ResponseEntity.ok(testService.savePassedTest(testDTO));
    }
}
