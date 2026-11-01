package com.example.readerai.repository;

import com.example.readerai.entity.ClassroomReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomReadingRepository extends JpaRepository<ClassroomReading, Long> {
    List<ClassroomReading> findByClassroom_IdOrderByCreatedAtDesc(Long classroomId);
}
