package com.example.readerai.repository;

import com.example.readerai.entity.ClassroomBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomBookRepository extends JpaRepository<ClassroomBook, Long> {
    List<ClassroomBook> findByClassroom_IdOrderByCreatedAtDesc(Long classroomId);

    boolean existsByClassroom_IdAndBook_Id(Long classroomId, Long bookId);

    void deleteByClassroom_Id(Long classroomId);
}

