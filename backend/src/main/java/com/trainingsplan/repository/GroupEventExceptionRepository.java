package com.trainingsplan.repository;

import com.trainingsplan.entity.GroupEventException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupEventExceptionRepository extends JpaRepository<GroupEventException, Long> {

    List<GroupEventException> findByEventId(Long eventId);

    Optional<GroupEventException> findByEventIdAndExceptionDate(Long eventId, LocalDate exceptionDate);

    boolean existsByEventIdAndExceptionDate(Long eventId, LocalDate exceptionDate);
}
