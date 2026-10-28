package com.example.readerai.repository;

import com.example.readerai.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByTeacherUserIdOrderByCreatedAtDesc(String teacherUserId);

    Optional<Classroom> findByCode(String code);

    boolean existsByCode(String code);
}
