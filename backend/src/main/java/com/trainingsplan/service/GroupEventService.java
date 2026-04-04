package com.trainingsplan.service;

import com.trainingsplan.dto.*;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.GroupEventRegistrationRepository;
import com.trainingsplan.repository.GroupEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class GroupEventService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final GroupEventRepository eventRepository;
    private final GroupEventRegistrationRepository registrationRepository;

    public GroupEventService(GroupEventRepository eventRepository,
                             GroupEventRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
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
        event.setMaxParticipants(request.maxParticipants());
        event.setCostCents(request.costCents());
        if (request.costCurrency() != null) {
            event.setCostCurrency(request.costCurrency());
        }
        if (request.difficulty() != null) {
            event.setDifficulty(GroupEventDifficulty.valueOf(request.difficulty()));
        }
        event.setStatus(GroupEventStatus.DRAFT);
        event.setCreatedAt(LocalDateTime.now());

        event = eventRepository.save(event);
        return toDto(event, null);
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
        if (request.maxParticipants() != null) event.setMaxParticipants(request.maxParticipants());
        if (request.costCents() != null) event.setCostCents(request.costCents());
        if (request.costCurrency() != null) event.setCostCurrency(request.costCurrency());
        if (request.difficulty() != null) event.setDifficulty(GroupEventDifficulty.valueOf(request.difficulty()));

        event.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(event);
        return toDto(event, null);
    }

    public GroupEventDto publishEvent(User trainer, Long eventId) {
        GroupEvent event = getOwnEvent(trainer, eventId);
        if (event.getStatus() != GroupEventStatus.DRAFT) {
            throw new IllegalStateException("Only draft events can be published");
        }
        event.setStatus(GroupEventStatus.PUBLISHED);
        event.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(event);
        return toDto(event, null);
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

    @Transactional(readOnly = true)
    public List<GroupEventDto> getTrainerEvents(User trainer) {
        return eventRepository.findByTrainerIdOrderByEventDateDesc(trainer.getId()).stream()
                .map(e -> toDto(e, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getNearbyEvents(double lat, double lon, double radiusKm, User currentUser) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<GroupEvent> events = eventRepository.findNearbyPublished(
                GroupEventStatus.PUBLISHED, LocalDate.now(),
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta);

        return events.stream()
                .filter(e -> haversineDistance(lat, lon, e.getLatitude(), e.getLongitude()) <= radiusKm)
                .map(e -> toDto(e, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getUpcomingEvents(User currentUser) {
        return eventRepository.findByStatusAndEventDateGreaterThanEqualOrderByEventDateAsc(
                GroupEventStatus.PUBLISHED, LocalDate.now()).stream()
                .map(e -> toDto(e, currentUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupEventDto getEventDetail(Long eventId, User currentUser) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        return toDto(event, currentUser);
    }

    public void registerForEvent(User user, Long eventId) {
        if (!user.isGroupEventsEnabled()) {
            throw new IllegalStateException("Group events feature is not enabled for this user");
        }

        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() != GroupEventStatus.PUBLISHED) {
            throw new IllegalStateException("Can only register for published events");
        }

        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot register for past events");
        }

        var existing = registrationRepository.findByEventIdAndUserId(eventId, user.getId());
        if (existing.isPresent() && existing.get().getStatus() == RegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Already registered for this event");
        }

        if (event.getMaxParticipants() != null) {
            int count = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
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
            reg.setRegisteredAt(LocalDateTime.now());
            reg.setStatus(RegistrationStatus.REGISTERED);
            registrationRepository.save(reg);
        }
    }

    public void cancelRegistration(User user, Long eventId) {
        GroupEventRegistration reg = registrationRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        reg.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public List<GroupEventDto> getMyRegistrations(User user) {
        return registrationRepository.findByUserIdAndStatusOrderByRegisteredAtDesc(user.getId(), RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> toDto(reg.getEvent(), user))
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

    private GroupEvent getOwnEvent(User trainer, Long eventId) {
        GroupEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (!event.getTrainer().getId().equals(trainer.getId()) && trainer.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("You can only manage your own events");
        }
        return event;
    }

    private GroupEventDto toDto(GroupEvent event, User currentUser) {
        int participants = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);
        boolean registered = false;
        if (currentUser != null) {
            var reg = registrationRepository.findByEventIdAndUserId(event.getId(), currentUser.getId());
            registered = reg.isPresent() && reg.get().getStatus() == RegistrationStatus.REGISTERED;
        }
        return new GroupEventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocationName(),
                event.getLatitude(),
                event.getLongitude(),
                event.getDistanceKm(),
                event.getMaxParticipants(),
                participants,
                event.getCostCents(),
                event.getCostCurrency(),
                event.getDifficulty() != null ? event.getDifficulty().name() : null,
                event.getStatus().name(),
                event.getTrainer().getUsername(),
                event.getTrainer().getId(),
                event.getCreatedAt(),
                registered
        );
    }

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
