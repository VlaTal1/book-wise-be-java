package com.example.readerai.controller;

import com.example.readerai.dto.CreateProfileRequest;
import com.example.readerai.dto.ParticipantDTO;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participant")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    // 404, якщо користувач ще не обрав роль після реєстрації.
    @GetMapping("/me")
    public ResponseEntity<ParticipantDTO> getCurrentProfile() {
        return ResponseEntity.ok(participantService.getCurrentProfile());
    }

    @PostMapping("/me")
    public ResponseEntity<ParticipantDTO> createProfile(@RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(participantService.createProfile(request));
    }

    // Вчитель — усі учні його класів, учень — лише він сам.
    @GetMapping("/students")
    public ResponseEntity<List<ParticipantDTO>> getStudents() {
        return ResponseEntity.ok(participantService.getVisibleStudentsDTO());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipantDTO> getParticipantById(@PathVariable Long id) throws NotFoundException {
        return ResponseEntity.ok(participantService.getById(id));
    }
}
