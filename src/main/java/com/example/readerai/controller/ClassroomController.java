package com.example.readerai.controller;

import com.example.readerai.dto.TestDTO;
import com.example.readerai.dto.classroom.ClassroomDTO;
import com.example.readerai.dto.classroom.ClassroomDetailsDTO;
import com.example.readerai.dto.classroom.ClassroomTestRequest;
import com.example.readerai.dto.classroom.CreateClassroomRequest;
import com.example.readerai.dto.classroom.JoinClassroomRequest;
import com.example.readerai.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    public ResponseEntity<ClassroomDTO> create(@RequestBody CreateClassroomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classroomService.create(request.getName(), request.getTargetWpm()));
    }

    @GetMapping
    public ResponseEntity<List<ClassroomDTO>> getMyClassrooms() {
        return ResponseEntity.ok(classroomService.getMyClassrooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomDetailsDTO> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(classroomService.getDetails(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomDetailsDTO> update(@PathVariable Long id, @RequestBody CreateClassroomRequest request) {
        return ResponseEntity.ok(classroomService.update(id, request.getName(), request.getTargetWpm()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classroomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    public ResponseEntity<ClassroomDTO> join(@RequestBody JoinClassroomRequest request) {
        return ResponseEntity.ok(classroomService.join(request.getCode()));
    }

    @PostMapping("/{id}/books/{bookId}")
    public ResponseEntity<ClassroomDetailsDTO> assignBook(@PathVariable Long id, @PathVariable Long bookId) {
        return ResponseEntity.ok(classroomService.assignBook(id, bookId));
    }

    @PostMapping("/{id}/tests/{classroomTestId}/regenerate")
    public ResponseEntity<ClassroomDetailsDTO> regenerateTest(@PathVariable Long id, @PathVariable Long classroomTestId) {
        return ResponseEntity.ok(classroomService.regenerateTest(id, classroomTestId));
    }

    @DeleteMapping("/{id}/tests/{classroomTestId}")
    public ResponseEntity<ClassroomDetailsDTO> deleteTest(@PathVariable Long id, @PathVariable Long classroomTestId) {
        return ResponseEntity.ok(classroomService.deleteTest(id, classroomTestId));
    }

    @PostMapping("/{id}/tests")
    public ResponseEntity<List<TestDTO>> createTest(@PathVariable Long id, @RequestBody ClassroomTestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classroomService.createTestForClassroom(id, request));
    }
}
