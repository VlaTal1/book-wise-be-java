package com.example.readerai.repository;

import com.example.readerai.entity.ReadingSpeedAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingSpeedAttemptRepository extends JpaRepository<ReadingSpeedAttempt, Long> {
    List<ReadingSpeedAttempt> findAllByParticipant_IdOrderByCreatedAtDesc(Long participantId);

    // Для батьківського режиму — історія по УСІХ дітях цього користувача
    // одним списком (Participant.userId — власник профілю дитини, той самий
    // user_id, що й у Supabase JWT автентифікованого батька).
    List<ReadingSpeedAttempt> findAllByParticipant_UserIdOrderByCreatedAtDesc(String userId);
}
