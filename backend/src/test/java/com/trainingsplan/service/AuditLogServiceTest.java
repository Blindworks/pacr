package com.trainingsplan.service;

import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository repository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditLogService(repository);
    }

    @Test
    void log_withActor_savesCorrectFields() {
        User actor = new User("testuser", "test@example.com", LocalDateTime.now());
        // Setze ID via Reflection, da kein Setter vorhanden
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(actor, 42L);
        } catch (Exception e) { throw new RuntimeException(e); }

        service.log(actor, AuditAction.LOGIN, null, null, null);

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(42L, saved.getActorId());
        assertEquals("testuser", saved.getActorUsername());
        assertEquals(AuditAction.LOGIN, saved.getAction());
        assertNull(saved.getTargetType());
    }

    @Test
    void log_withoutActor_savesNullActorFields() {
        service.log(null, AuditAction.PLAN_CREATED, "TRAINING_PLAN", "5", null);

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertNull(saved.getActorId());
        assertNull(saved.getActorUsername());
        assertEquals("TRAINING_PLAN", saved.getTargetType());
        assertEquals("5", saved.getTargetId());
    }

    @Test
    void log_withDetails_serialisesAsJson() {
        service.log(null, AuditAction.USER_UPDATED, "USER", "1",
                Map.of("changedFields", "status"));

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getDetails());
        assertTrue(captor.getValue().getDetails().contains("changedFields"));
    }
}
