package com.example.readerai.service;

import com.example.readerai.converter.ParticipantConverter;
import com.example.readerai.dto.CreateProfileRequest;
import com.example.readerai.dto.ParticipantDTO;
import com.example.readerai.entity.Participant;
import com.example.readerai.entity.UserRole;
import com.example.readerai.exception.NotFoundException;
import com.example.readerai.exception.PermissionDeniedException;
import com.example.readerai.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    private final ParticipantConverter participantConverter;

    private final UserService userService;

    // Профіль створюється один раз після реєстрації в Supabase — користувач
    // сам обирає, хто він: вчитель чи учень.
    public ParticipantDTO createProfile(CreateProfileRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role cannot be empty");
        }

        String userId = userService.getUserId();
        if (participantRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("Profile already exists");
        }

        Participant participant = Participant.builder()
                .name(request.getName().trim())
                .role(request.getRole())
                .userId(userId)
                .build();
        participant = participantRepository.save(participant);

        return participantConverter.toDTO(participant);
    }

    public ParticipantDTO getCurrentProfile() {
        return participantConverter.toDTO(getCurrent());
    }

    public Participant getCurrent() {
        return participantRepository.findByUserId(userService.getUserId())
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    public Participant requireTeacher() {
        Participant current = getCurrent();
        if (current.getRole() != UserRole.TEACHER) {
            throw new PermissionDeniedException("Only teachers can do this");
        }
        return current;
    }

    public Participant requireStudent() {
        Participant current = getCurrent();
        if (current.getRole() != UserRole.STUDENT) {
            throw new PermissionDeniedException("Only students can do this");
        }
        return current;
    }

    // Учні, чиї дані бачить поточний користувач: вчитель — усіх учнів своїх
    // класів, учень — лише себе.
    public List<Participant> getVisibleStudents() {
        Participant current = getCurrent();
        if (current.getRole() == UserRole.TEACHER) {
            return participantRepository.findByClassroom_TeacherUserId(current.getUserId());
        }
        return List.of(current);
    }

    public List<ParticipantDTO> getVisibleStudentsDTO() {
        return getVisibleStudents().stream().map(participantConverter::toDTO).toList();
    }

    public List<Long> getVisibleStudentIds() {
        return getVisibleStudents().stream().map(Participant::getId).toList();
    }

    // Доступ до даних учня має він сам і вчитель його класу.
    public boolean canView(Participant participant) {
        String userId = userService.getUserId();
        if (Objects.equals(participant.getUserId(), userId)) {
            return true;
        }
        return participant.getClassroom() != null
                && Objects.equals(participant.getClassroom().getTeacherUserId(), userId);
    }

    public void assertCanView(Participant participant) {
        if (!canView(participant)) {
            throw new PermissionDeniedException("You do not have permission to access this participant");
        }
    }

    public Participant findVisible(Long id) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Participant with id " + id + " not found"));
        assertCanView(participant);
        return participant;
    }

    public ParticipantDTO getById(Long id) throws NotFoundException {
        return participantConverter.toDTO(findVisible(id));
    }
}
