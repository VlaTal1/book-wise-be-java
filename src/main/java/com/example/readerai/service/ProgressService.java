package com.example.readerai.service;

import com.example.readerai.converter.ProgressConverter;
import com.example.readerai.dto.ProgressDTO;
import com.example.readerai.dto.ReadingStatus;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.Progress;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRepository progressRepository;

    private final ProgressConverter progressConverter;

    private final ParticipantService participantService;

    private final UserService userService;

    public ProgressDTO getProgressByBookIdAndParticipantId(Long bookId, Long participantId) throws NotFoundException {
        participantService.findVisible(participantId);
        Progress progress = progressRepository.findByBookIdAndParticipantId(bookId, participantId);
        if (progress == null) {
            throw new NotFoundException("Progress with book id: " + bookId + " and participant id: " + participantId + " not found");
        }
        return progressConverter.toDTO(progress);
    }

    public void createIfNotExists(Book book, Participant participant) {
        if (progressRepository.findByBookIdAndParticipantId(book.getId(), participant.getId()) != null) {
            return;
        }

        Progress progress = Progress.builder()
                .book(book)
                .participant(participant)
                .readPages(0)
                .currentPage(0)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .status(ReadingStatus.NOT_STARTED.toString())
                .build();

        progressRepository.save(progress);
    }

    public ProgressDTO updateProgress(ProgressDTO progressDTO) {
        Progress progress = progressRepository.findById(progressDTO.getId()).orElseThrow(
                () -> new NotFoundException("Progress with id " + progressDTO.getId() + " not found")
        );
        if (!Objects.equals(progress.getParticipant().getUserId(), userService.getUserId())) {
            throw new PermissionDeniedException("Only the student can update own progress");
        }

        if (Objects.equals(progress.getStatus(), ReadingStatus.NOT_STARTED.toString())) {
            progress.setStatus(ReadingStatus.IN_PROGRESS.toString());
        }

        progress.setReadPages(progressDTO.getReadPages());
        progress.setCurrentPage(progressDTO.getCurrentPage());

        Progress updatedProgress = progressRepository.save(progress);
        return progressConverter.toDTO(updatedProgress);
    }
}
