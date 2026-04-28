package com.trainingsplan.service;

import com.trainingsplan.dto.*;
import com.trainingsplan.entity.*;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.GroupEventExceptionRepository;
import com.trainingsplan.repository.GroupEventRegistrationRepository;
import com.trainingsplan.repository.GroupEventRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupEventService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int RECURRENCE_LOOKAHEAD_MONTHS = 3;

    private final GroupEventRepository eventRepository;
    private final GroupEventRegistrationRepository registrationRepository;
    private final GroupEventExceptionRepository exceptionRepository;
    private final RecurrenceService recurrenceService;
    private final ImageStoragePort imageStoragePort;

    public GroupEventService(GroupEventRepository eventRepository,
                             GroupEventRegistrationRepository registrationRepository,
                             GroupEventExceptionRepository exceptionRepository,
                             RecurrenceService recurrenceService,
                             ImageStoragePort imageStoragePort) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.exceptionRepository = exceptionRepository;
        this.recurrenceService = recurrenceService;
        this.imageStoragePort = imageStoragePort;
    }

    public GroupEventDto createEvent(User trainer, CreateGroupEventRequest request) {
        if (trainer.getRole() != UserRole.TRAINER && trainer.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Only trainers can create events");
        }

        GroupEvent event = new GroupEvent();
        event.setTrainer(trainer);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setLocationName(request.locationName());
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setDistanceKm(request.distanceKm());
        event.setPaceMinSecondsPerKm(request.paceMinSecondsPerKm());
        event.setPaceMaxSecondsPerKm(request.paceMaxSecondsPerKm());
        event.setMaxParticipants(request.maxParticipants());
        event.setCostCents(request.costCents());
        if (request.costCurrency() != null) {
            event.setCostCurrency(request.costCurrency());
        }
        if (request.difficulty() != null) {
            event.setDifficulty(GroupEventDifficulty.valueOf(request.difficulty()));
        }
        event.setRrule(request.rrule());
        event.setRecurrenceEndDate(request.recurrenceEndDate());
        event.setStatus(GroupEventStatus.DRAFT);
        event.setCreatedAt(LocalDateTime.now());

        event = eventRepository.save(event);
        return toDto(event, null, null);
    }

    public GroupEventDto updateEvent(User trainer, Long eventId, UpdateGroupEventRequest request) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (event.getStatus() == GroupEventStatus.CANCELLED || event.getStatus() == GroupEventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update a cancelled or completed event");
        }

        if (request.title() != null) event.setTitle(request.title());
        if (request.description() != null) event.setDescription(request.description());
        if (request.eventDate() != null) event.setEventDate(request.eventDate());
        if (request.startTime() != null) event.setStartTime(request.startTime());
        if (request.endTime() != null) event.setEndTime(request.endTime());
        if (request.locationName() != null) event.setLocationName(request.locationName());
        if (request.latitude() != null) event.setLatitude(request.latitude());
        if (request.longitude() != null) event.setLongitude(request.longitude());
        if (request.distanceKm() != null) event.setDistanceKm(request.distanceKm());
        if (request.paceMinSecondsPerKm() != null) event.setPaceMinSecondsPerKm(request.paceMinSecondsPerKm());
        if (request.paceMaxSecondsPerKm() != null) event.setPaceMaxSecondsPerKm(request.paceMaxSecondsPerKm());
        if (request.maxParticipants() != null) event.setMaxParticipants(request.maxParticipants());
        if (request.costCents() != null) event.setCostCents(request.costCents());
        if (request.costCurrency() != null) event.setCostCurrency(request.costCurrency());
        if (request.difficulty() != null) event.setDifficulty(GroupEventDifficulty.valueOf(request.difficulty()));
        if (request.rrule() != null) event.setRrule(request.rrule().isBlank() ? null : request.rrule());
        if (request.recurrenceEndDate() != null) event.setRecurrenceEndDate(request.recurrenceEndDate());

        event.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(event);
        return toDto(event, null, null);
    }

    public GroupEventDto publishEvent(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (event.getStatus() != GroupEventStatus.DRAFT) {
            throw new IllegalStateException("Only draft events can be published");
        }
        event.setStatus(GroupEventStatus.PUBLISHED);
        event.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(event);
        return toDto(event, null, null);
    }

    public void cancelEvent(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (event.getStatus() == GroupEventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed event");
        }
        event.setStatus(GroupEventStatus.CANCELLED);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    public void deleteEvent(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (event.getStatus() != GroupEventStatus.DRAFT) {
            throw new IllegalStateException("Only draft events can be deleted");
        }
        eventRepository.delete(event);
    }

    public void cancelOccurrence(User trainer, Long eventId, LocalDate date, String reason) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (!event.isRecurring()) {
            throw new IllegalStateException("Event is not recurring");
        }
        if (exceptionRepository.existsByEventIdAndExceptionDate(eventId, date)) {
            throw new IllegalStateException("This occurrence is already cancelled");
        }
        GroupEventException exception = new GroupEventException();
        exception.setEvent(event);
        exception.setExceptionDate(date);
        exception.setReason(reason);
        exception.setCreatedAt(LocalDateTime.now());
        exceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getTrainerEvents(User trainer) {
        return eventRepository.findByTrainerIdOrderByEventDateDesc(trainer.getId()).stream()
                .map(e -> toDto(e, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getNearbyEvents(double lat, double lon, double radiusKm, User currentUser) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusMonths(RECURRENCE_LOOKAHEAD_MONTHS);

        // One-off events
        List<GroupEvent> oneOffEvents = eventRepository.findNearbyPublished(
                GroupEventStatus.PUBLISHED, today,
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta);

        List<GroupEventDto> result = new ArrayList<>(oneOffEvents.stream()
                .filter(e -> !e.isRecurring())
                .filter(e -> haversineDistance(lat, lon, e.getLatitude(), e.getLongitude()) <= radiusKm)
                .map(e -> toDto(e, currentUser, null))
                .toList());

        // Recurring events
        List<GroupEvent> recurringEvents = eventRepository.findNearbyPublishedRecurringInRange(
                GroupEventStatus.PUBLISHED, today, rangeEnd,
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta);

        for (GroupEvent event : recurringEvents) {
            if (haversineDistance(lat, lon, event.getLatitude(), event.getLongitude()) <= radiusKm) {
                result.addAll(expandRecurringEvent(event, today, rangeEnd, currentUser));
            }
        }

        result.sort(Comparator.comparing(GroupEventDto::eventDate));
        return result;
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getUpcomingEvents(User currentUser) {
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusMonths(RECURRENCE_LOOKAHEAD_MONTHS);

        // One-off events
        List<GroupEventDto> result = new ArrayList<>(
                eventRepository.findByStatusAndEventDateGreaterThanEqualOrderByEventDateAsc(
                        GroupEventStatus.PUBLISHED, today).stream()
                        .filter(e -> !e.isRecurring())
                        .map(e -> toDto(e, currentUser, null))
                        .toList());

        // Recurring events
        List<GroupEvent> recurringEvents = eventRepository.findPublishedRecurringInRange(
                GroupEventStatus.PUBLISHED, today, rangeEnd);

        for (GroupEvent event : recurringEvents) {
            result.addAll(expandRecurringEvent(event, today, rangeEnd, currentUser));
        }

        result.sort(Comparator.comparing(GroupEventDto::eventDate));
        return result;
    }

    @Transactional(readOnly = true)
    public GroupEventDto getEventDetail(Long eventId, User currentUser) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        return toDto(event, currentUser, null);
    }

    @Transactional(readOnly = true)
    public GroupEventDto getEventOccurrenceDetail(Long eventId, LocalDate occurrenceDate, User currentUser) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (!event.isRecurring()) {
            return toDto(event, currentUser, null);
        }
        return toDto(event, currentUser, occurrenceDate);
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getEventOccurrences(Long eventId, LocalDate from, LocalDate to) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (!event.isRecurring()) {
            return List.of(toDto(event, null, null));
        }
        return expandRecurringEvent(event, from, to, null);
    }

    public void registerForEvent(User user, Long eventId, LocalDate occurrenceDate) {
        if (!user.isGroupEventsEnabled()) {
            throw new IllegalStateException("Group events feature is not enabled for this user");
        }

        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() != GroupEventStatus.PUBLISHED) {
            throw new IllegalStateException("Can only register for published events");
        }

        // Determine the effective date for this registration
        LocalDate effectiveDate = event.isRecurring() ? occurrenceDate : event.getEventDate();
        if (effectiveDate != null && effectiveDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot register for past events");
        }

        // For recurring events, check that the occurrence isn't cancelled
        if (event.isRecurring() && occurrenceDate != null) {
            if (exceptionRepository.existsByEventIdAndExceptionDate(eventId, occurrenceDate)) {
                throw new IllegalStateException("This occurrence has been cancelled");
            }
        }

        // Check existing registration
        Optional<GroupEventRegistration> existing;
        if (event.isRecurring() && occurrenceDate != null) {
            existing = registrationRepository.findByEventIdAndUserIdAndOccurrenceDate(eventId, user.getId(), occurrenceDate);
        } else {
            existing = registrationRepository.findByEventIdAndUserIdAndOccurrenceDate(eventId, user.getId(), null);
        }

        if (existing.isPresent() && existing.get().getStatus() == RegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Already registered for this event");
        }

        // Check capacity
        if (event.getMaxParticipants() != null) {
            int count;
            if (event.isRecurring() && occurrenceDate != null) {
                count = registrationRepository.countByEventIdAndOccurrenceDateAndStatus(eventId, occurrenceDate, RegistrationStatus.REGISTERED);
            } else {
                count = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
            }
            if (count >= event.getMaxParticipants()) {
                throw new IllegalStateException("Event is full");
            }
        }

        if (existing.isPresent()) {
            GroupEventRegistration reg = existing.get();
            reg.setStatus(RegistrationStatus.REGISTERED);
            reg.setRegisteredAt(LocalDateTime.now());
            registrationRepository.save(reg);
        } else {
            GroupEventRegistration reg = new GroupEventRegistration();
            reg.setEvent(event);
            reg.setUser(user);
            reg.setOccurrenceDate(event.isRecurring() ? occurrenceDate : null);
            reg.setRegisteredAt(LocalDateTime.now());
            reg.setStatus(RegistrationStatus.REGISTERED);
            registrationRepository.save(reg);
        }
    }

    public void cancelRegistration(User user, Long eventId, LocalDate occurrenceDate) {
        Optional<GroupEventRegistration> regOpt;
        if (occurrenceDate != null) {
            regOpt = registrationRepository.findByEventIdAndUserIdAndOccurrenceDate(eventId, user.getId(), occurrenceDate);
        } else {
            regOpt = registrationRepository.findByEventIdAndUserIdAndOccurrenceDate(eventId, user.getId(), null);
        }
        GroupEventRegistration reg = regOpt
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        reg.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getMyRegistrations(User user) {
        return registrationRepository.findByUserIdAndStatusOrderByRegisteredAtDesc(user.getId(), RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> toDto(reg.getEvent(), user, reg.getOccurrenceDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupEventRegistrationDto> getEventParticipants(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        return registrationRepository.findByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> new GroupEventRegistrationDto(
                        reg.getId(),
                        reg.getEvent().getId(),
                        reg.getEvent().getTitle(),
                        reg.getUser().getId(),
                        reg.getUser().getUsername(),
                        reg.getStatus().name(),
                        reg.getRegisteredAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupEventRegistrationDto> getOccurrenceParticipants(User trainer, Long eventId, LocalDate occurrenceDate) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        return registrationRepository.findByEventIdAndOccurrenceDateAndStatus(event.getId(), occurrenceDate, RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> new GroupEventRegistrationDto(
                        reg.getId(),
                        reg.getEvent().getId(),
                        reg.getEvent().getTitle(),
                        reg.getUser().getId(),
                        reg.getUser().getUsername(),
                        reg.getStatus().name(),
                        reg.getRegisteredAt()))
                .toList();
    }

    // --- Private helpers ---

    private List<GroupEventDto> expandRecurringEvent(GroupEvent event, LocalDate rangeStart, LocalDate rangeEnd, User currentUser) {
        Set<LocalDate> exceptions = exceptionRepository.findByEventId(event.getId()).stream()
                .map(GroupEventException::getExceptionDate)
                .collect(Collectors.toSet());

        LocalDate effectiveEnd = rangeEnd;
        if (event.getRecurrenceEndDate() != null && event.getRecurrenceEndDate().isBefore(effectiveEnd)) {
            effectiveEnd = event.getRecurrenceEndDate();
        }

        List<LocalDate> occurrences = recurrenceService.expandOccurrences(
                event.getRrule(), event.getEventDate(), rangeStart, effectiveEnd, exceptions);

        return occurrences.stream()
                .map(date -> toDto(event, currentUser, date))
                .toList();
    }

    private GroupEvent getOwnEvent(User trainer, Long eventId) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (!event.getTrainer().getId().equals(trainer.getId()) && trainer.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("You can only manage your own events");
        }
        return event;
    }

    private GroupEventDto toDto(GroupEvent event, User currentUser, LocalDate occurrenceDate) {
        int participants;
        boolean registered = false;

        if (event.isRecurring() && occurrenceDate != null) {
            participants = registrationRepository.countByEventIdAndOccurrenceDateAndStatus(
                    event.getId(), occurrenceDate, RegistrationStatus.REGISTERED);
            if (currentUser != null) {
                var reg = registrationRepository.findByEventIdAndUserIdAndOccurrenceDate(
                        event.getId(), currentUser.getId(), occurrenceDate);
                registered = reg.isPresent() && reg.get().getStatus() == RegistrationStatus.REGISTERED;
            }
        } else {
            participants = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);
            if (currentUser != null) {
                var reg = registrationRepository.findByEventIdAndUserId(event.getId(), currentUser.getId());
                registered = reg.isPresent() && reg.get().getStatus() == RegistrationStatus.REGISTERED;
            }
        }

        return new GroupEventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.isRecurring() && occurrenceDate != null ? occurrenceDate : event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocationName(),
                event.getLatitude(),
                event.getLongitude(),
                event.getDistanceKm(),
                event.getPaceMinSecondsPerKm(),
                event.getPaceMaxSecondsPerKm(),
                event.getMaxParticipants(),
                participants,
                event.getCostCents(),
                event.getCostCurrency(),
                event.getDifficulty() != null ? event.getDifficulty().name() : null,
                event.getStatus().name(),
                event.getTrainer().getUsername(),
                event.getTrainer().getId(),
                event.getCreatedAt(),
                registered,
                event.getRrule(),
                event.getRecurrenceEndDate(),
                occurrenceDate,
                event.isRecurring(),
                event.getEventImageFilename()
        );
    }

    public void uploadEventImage(User trainer, Long eventId, MultipartFile file) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        String oldFilename = event.getEventImageFilename();
        String newFilename = imageStoragePort.store(file);
        event.setEventImageFilename(newFilename);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        if (oldFilename != null && !oldFilename.isBlank() && !oldFilename.equals(newFilename)) {
            imageStoragePort.delete(oldFilename);
        }
    }

    public void deleteEventImage(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        String oldFilename = event.getEventImageFilename();
        if (oldFilename == null || oldFilename.isBlank()) {
            return;
        }
        event.setEventImageFilename(null);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        imageStoragePort.delete(oldFilename);
    }

    @Transactional(readOnly = true)
    public EventImageData loadEventImage(Long eventId) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        String filename = event.getEventImageFilename();
        if (filename == null || filename.isBlank()) {
            return null;
        }
        Resource resource = imageStoragePort.load(filename);
        String contentType = "application/octet-stream";
        try {
            String detectedType = Files.probeContentType(resource.getFile().toPath());
            if (detectedType != null && !detectedType.isBlank()) {
                contentType = detectedType;
            }
        } catch (IOException ignored) {
        }
        return new EventImageData(resource, contentType);
    }

    public record EventImageData(Resource resource, String contentType) {}

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
