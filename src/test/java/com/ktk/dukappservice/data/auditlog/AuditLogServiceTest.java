package com.ktk.dukappservice.data.auditlog;

import com.ktk.dukappservice.data.users.UserRepository;
import com.ktk.dukappservice.enums.AuditAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceTest {

    @Mock AuditLogRepository repository;
    @Mock UserRepository userRepository;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(repository, userRepository, JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordCapturesActorRequestAndDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/dukapp/api/donations/5");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));
        when(userRepository.findIdsByUsernameOrEmail("admin")).thenReturn(List.of(1L));

        service.record(AuditAction.DELETE, "Donation", 5L, Map.of("amount", 100));
        service.flush();

        AuditLog entry = savedEntries().getFirst();
        assertThat(entry.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(entry.getEntityType()).isEqualTo("Donation");
        assertThat(entry.getEntityId()).isEqualTo("5");
        assertThat(entry.getUsername()).isEqualTo("admin");
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(entry.getRequest()).isEqualTo("DELETE /dukapp/api/donations/5");
        assertThat(entry.getDetails()).isEqualTo("{\"amount\":100}");
        assertThat(entry.getTimestamp()).isNotNull();
    }

    @Test
    void actorIsSystemOutsideRequestsAndAnonymousInsideUnauthenticatedRequests() {
        service.record(AuditAction.CREATE, "Transaction", 1L, null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/dukapp/api/payments")));
        service.record(AuditAction.CREATE, "Payment", 2L, null);
        service.flush();

        List<AuditLog> entries = savedEntries();
        assertThat(entries).extracting(AuditLog::getUsername).containsExactly(AuditLogService.SYSTEM, AuditLogService.ANONYMOUS);
        assertThat(entries).extracting(AuditLog::getUserId).containsOnlyNulls();
        verifyNoInteractions(userRepository);
    }

    @Test
    void failedLoginKeepsTriedUsernameAndLeavesUserIdEmptyWhenAmbiguous() {
        when(userRepository.findIdsByUsernameOrEmail("shared@mail.com")).thenReturn(List.of(1L, 2L));

        service.recordAs("shared@mail.com", AuditAction.LOGIN_FAILED, null, null, Map.of("reason", "BadCredentialsException"));
        service.flush();

        AuditLog entry = savedEntries().getFirst();
        assertThat(entry.getUsername()).isEqualTo("shared@mail.com");
        assertThat(entry.getUserId()).isNull();
    }

    @Test
    void flushWithNothingQueuedDoesNotTouchTheDatabase() {
        service.flush();
        verifyNoInteractions(repository);
    }

    @Test
    void flushSwallowsDatabaseErrors() {
        when(repository.saveAll(anyList())).thenThrow(new RuntimeException("db down"));
        service.record(AuditAction.LOGOUT, null, null, null);

        service.flush();

        verify(repository).saveAll(anyList());
    }

    @Test
    void dateFiltersAreInclusiveDaysWithDefaults() {
        service.fetchByQuery("2026-09-01", "2026-09-30", null, null, " ", "", null, null, null);
        verify(repository).fetchByQuery(eq(LocalDate.of(2026, 9, 1).atStartOfDay()), eq(LocalDate.of(2026, 10, 1).atStartOfDay()),
                isNull(), isNull(), eq(""), isNull(), isNull(), eq(""), isNull());

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        service.fetchByQuery(null, "2026-09-30T12:15:00", AuditAction.LOGIN, 3L, "adm", "User", "3", "x", null);
        verify(repository).fetchByQuery(from.capture(), to.capture(), eq(AuditAction.LOGIN), eq(3L), eq("adm"), eq("User"), eq("3"), eq("x"), isNull());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
        assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 15));
    }

    @Test
    void invalidDateIsBadRequest() {
        assertThatThrownBy(() -> service.fetchByQuery("yesterday", null, null, null, null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid date");
    }

    @SuppressWarnings("unchecked")
    private List<AuditLog> savedEntries() {
        ArgumentCaptor<List<AuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return List.copyOf(captor.getValue());
    }
}
