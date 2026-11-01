package com.example.readerai.config.security;

import com.example.readerai.entity.Participant;
import com.example.readerai.repository.ParticipantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActAsStudentFilterTest {

    private final ParticipantRepository repository = mock(ParticipantRepository.class);
    private final ActAsStudentFilter filter = new ActAsStudentFilter(repository);

    @BeforeEach
    void authenticateTeacher() {
        User teacher = new User("teacher-1", "", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(teacher, null, teacher.getAuthorities()));
        when(repository.findByIdAndLocalTrueAndClassroom_TeacherUserId(anyLong(), any())).thenReturn(Optional.empty());
        when(repository.findByIdAndLocalTrueAndClassroom_TeacherUserId(7L, "teacher-1"))
                .thenReturn(Optional.of(Participant.builder().id(7L).userId("local:abc").local(true).build()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private String principalAfter(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                principal = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
            }
        };
        principal = null;
        filter.doFilter(request, response, chain);
        return principal;
    }

    private String principal;

    @Test
    void withoutHeaderTeacherStaysTeacher() throws Exception {
        assertThat(principalAfter(new MockHttpServletRequest(), new MockHttpServletResponse())).isEqualTo("teacher-1");
    }

    @Test
    void teacherActsAsOwnLocalStudent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ActAsStudentFilter.HEADER, "7");
        assertThat(principalAfter(request, new MockHttpServletResponse())).isEqualTo("local:abc");
    }

    @Test
    void foreignOrRegisteredStudentIsForbidden() throws Exception {
        for (String header : new String[]{"8", "not-a-number"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(ActAsStudentFilter.HEADER, header);
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThat(principalAfter(request, response)).isNull();
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }

    @Test
    void headerWithoutAuthenticationIsIgnored() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ActAsStudentFilter.HEADER, "7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
