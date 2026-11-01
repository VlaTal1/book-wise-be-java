package com.example.readerai.repository;

import com.example.readerai.entity.ReadingSpeedAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReadingSpeedAttemptRepository extends JpaRepository<ReadingSpeedAttempt, Long> {
    List<ReadingSpeedAttempt> findAllByParticipant_IdOrderByCreatedAtDesc(Long participantId);

    // Історія одразу по кількох учнях (для вчителя — усі учні його класів).
    List<ReadingSpeedAttempt> findAllByParticipant_IdInOrderByCreatedAtDesc(Collection<Long> participantIds);

    List<ReadingSpeedAttempt> findAllByClassroomReading_Id(Long classroomReadingId);

    List<ReadingSpeedAttempt> findAllByClassroomReading_IdAndParticipant_Id(Long classroomReadingId, Long participantId);

    long countByClassroomReading_IdAndParticipant_Id(Long classroomReadingId, Long participantId);
}
