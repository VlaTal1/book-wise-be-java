package com.example.readerai.controller;

import com.example.readerai.dto.statistics.GradeByParticipant;
import com.example.readerai.dto.statistics.ReadingSpeedDailyStats;
import com.example.readerai.dto.statistics.ReadingSpeedStatsByParticipant;
import com.example.readerai.dto.statistics.ReadingStatsByDay;
import com.example.readerai.dto.statistics.WeeklyStatsByParticipant;
import com.example.readerai.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/participants/avgGrade")
    public ResponseEntity<List<GradeByParticipant>> getParticipantsGrade() {
        return ResponseEntity.ok(statisticsService.getParticipantsGrade());
    }

    @GetMapping("/participants/daily")
    public ResponseEntity<List<ReadingStatsByDay>> getDailyReadingStatistics() {
        return ResponseEntity.ok(statisticsService.getReadingStatsByDay());
    }

    @GetMapping("/participants/weekly")
    public ResponseEntity<List<WeeklyStatsByParticipant>> weeklyStatistics() {
        return ResponseEntity.ok(statisticsService.getWeeklyStats());
    }

    @GetMapping("/participants/reading-speed")
    public ResponseEntity<List<ReadingSpeedStatsByParticipant>> getReadingSpeedStats() {
        return ResponseEntity.ok(statisticsService.getReadingSpeedStats());
    }

    @GetMapping("/participants/{participantId}/reading-speed/daily")
    public ResponseEntity<List<ReadingSpeedDailyStats>> getReadingSpeedDailyStats(@PathVariable Long participantId) {
        return ResponseEntity.ok(statisticsService.getReadingSpeedDailyStats(participantId));
    }
}
