package com.example.readerai.repository;

import com.example.readerai.entity.ReadingSpeedAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingSpeedAttemptRepository extends JpaRepository<ReadingSpeedAttempt, Long> {
    List<ReadingSpeedAttempt> findAllByParticipant_IdOrderByCreatedAtDesc(Long participantId);
}
