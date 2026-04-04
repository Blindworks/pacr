package com.trainingsplan.repository;

import com.trainingsplan.entity.GroupEventRegistration;
import com.trainingsplan.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupEventRegistrationRepository extends JpaRepository<GroupEventRegistration, Long> {

    Optional<GroupEventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    Optional<GroupEventRegistration> findByEventIdAndUserIdAndOccurrenceDate(Long eventId, Long userId, LocalDate occurrenceDate);

    int countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    int countByEventIdAndOccurrenceDateAndStatus(Long eventId, LocalDate occurrenceDate, RegistrationStatus status);

    List<GroupEventRegistration> findByUserIdAndStatusOrderByRegisteredAtDesc(Long userId, RegistrationStatus status);

    List<GroupEventRegistration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    List<GroupEventRegistration> findByEventIdAndOccurrenceDateAndStatus(Long eventId, LocalDate occurrenceDate, RegistrationStatus status);
}
