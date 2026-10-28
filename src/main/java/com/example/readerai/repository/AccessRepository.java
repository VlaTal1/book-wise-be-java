package com.example.readerai.repository;

import com.example.readerai.entity.Access;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRepository extends JpaRepository<Access, Long> {
    List<Access> findByBookId(Long bookId);
    List<Access> findByParticipant_Id(Long participantId);
    Access findByBook_IdAndParticipant_Id(Long bookId, Long participantId);
    boolean existsByBook_IdAndParticipant_UserId(Long bookId, String userId);
    void deleteByParticipant_Id(Long participantId);
}
