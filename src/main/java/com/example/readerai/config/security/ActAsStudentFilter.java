package com.example.readerai.config.security;

import com.example.readerai.entity.Participant;
import com.example.readerai.repository.ParticipantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Режим учня на телефоні вчителя: запит із заголовком X-Act-As-Participant
 * виконується від імені локального учня (без власного акаунта), якщо
 * автентифікований користувач — вчитель класу цього учня. Підміняємо
 * principal на синтетичний userId учня — далі весь код (getCurrent,
 * перевірки доступу до книг, прогресу, тестів, читання) працює так, наче
 * учень увійшов сам, і вчительські дії в цьому режимі недоступні.
 */
public class ActAsStudentFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Act-As-Participant";

    private static final Logger log = LoggerFactory.getLogger(ActAsStudentFilter.class);

    private final ParticipantRepository participantRepository;

    public ActAsStudentFilter(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (header == null || header.isBlank() || authentication == null || !(authentication.getPrincipal() instanceof User teacher)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Participant> student;
        try {
            student = participantRepository.findByIdAndLocalTrueAndClassroom_TeacherUserId(Long.parseLong(header.trim()), teacher.getUsername());
        } catch (NumberFormatException e) {
            student = Optional.empty();
        }
        if (student.isEmpty()) {
            log.warn("User {} tried to act as participant {} without permission", teacher.getUsername(), header);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot act as this participant");
            return;
        }

        User actingUser = new User(student.get().getUserId(), "", Collections.emptyList());
        UsernamePasswordAuthenticationToken acting = new UsernamePasswordAuthenticationToken(actingUser, null, actingUser.getAuthorities());
        acting.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(acting);

        filterChain.doFilter(request, response);
    }
}
