package com.example.readerai.repository;

import com.example.readerai.entity.ClassroomTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomTestRepository extends JpaRepository<ClassroomTest, Long> {
    List<ClassroomTest> findByClassroom_Id(Long classroomId);

    List<ClassroomTest> findByClassroom_IdOrderByCreatedAtDesc(Long classroomId);
}
