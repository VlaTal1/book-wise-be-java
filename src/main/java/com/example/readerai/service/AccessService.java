package com.example.readerai.service;

import com.example.readerai.entity.Access;
import com.example.readerai.entity.Book;
import com.example.readerai.entity.Participant;
import com.example.readerai.repository.AccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessService {

    private final AccessRepository accessRepository;

    private final ProgressService progressService;

    // Викликається з ClassroomService (призначення книги класу / вступ учня),
    // права вчителя там уже перевірено.
    public void grantAccess(Book book, Participant participant) {
        Access access = accessRepository.findByBook_IdAndParticipant_Id(book.getId(), participant.getId());
        if (access == null) {
            accessRepository.save(Access.builder()
                    .participant(participant)
                    .book(book)
                    .build());
        }

        progressService.createIfNotExists(book, participant);
    }

    // Прогрес і тести лишаються як історія, зникає лише книга зі списку учня.
    public void revokeAllAccesses(Participant participant) {
        accessRepository.deleteByParticipant_Id(participant.getId());
    }
}
