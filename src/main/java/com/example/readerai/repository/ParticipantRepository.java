package com.example.readerai.repository;

import com.example.readerai.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByUserId(String userId);

    List<Participant> findByClassroom_IdOrderByName(Long classroomId);

    // Усі учні з класів цього вчителя.
    List<Participant> findByClassroom_TeacherUserId(String teacherUserId);
}
