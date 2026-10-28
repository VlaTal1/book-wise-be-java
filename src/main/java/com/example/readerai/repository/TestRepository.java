package com.example.readerai.repository;

import com.example.readerai.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findAllByProgress_Participant_Id(Long participantId);

    List<Test> findAllByProgress_Participant_IdAndProgress_Book_Id(Long participantId, Long bookId);
    List<Test> findAllByProgress_Participant_IdIn(Collection<Long> participantIds);

    List<Test> findAllByClassroomTest_Id(Long classroomTestId);

    boolean existsByClassroomTest_IdAndProgress_Participant_Id(Long classroomTestId, Long participantId);

    @Query("SELECT t FROM Test t " +
            "WHERE t.progress.participant.id = :participantId " +
            "AND t.completed = :completed " +
            "AND YEAR(t.progress.endDate) = :year")
    List<Test> findAllByProgress_Participant_IdAndCompletedAndYear(
            @Param("participantId") Long participantId,
            @Param("completed") String completed,
            @Param("year") int year);

    @Query(value = """
            SELECT t.* FROM test t
            JOIN progress p ON t.progress_id = p.id
            WHERE t.completed = 'NOT_STARTED'
            AND t.generation_status = 'READY'
            AND p.participant_id = :participantId
            AND p.book_id = :bookId
            ORDER BY t.end_page
            LIMIT 1
            """,
            nativeQuery = true)
    Test getFirstTestByParticipantIdAndBookId(@Param("participantId") Long participantId, @Param("bookId") Long bookId);

}
