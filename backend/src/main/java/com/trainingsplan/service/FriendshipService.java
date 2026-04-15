package com.trainingsplan.service;

import com.trainingsplan.dto.FriendActivityDto;
import com.trainingsplan.dto.FriendshipDto;
import com.trainingsplan.dto.LiveTrainingFriendDto;
import com.trainingsplan.dto.UserSearchResultDto;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.Friendship;
import com.trainingsplan.entity.FriendshipStatus;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserTrainingEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.repository.ActivityStreamRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.FriendshipRepository;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final UserTrainingEntryRepository userTrainingEntryRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FriendshipService(FriendshipRepository friendshipRepository,
                              UserRepository userRepository,
                              CompletedTrainingRepository completedTrainingRepository,
                              UserTrainingEntryRepository userTrainingEntryRepository,
                              ActivityStreamRepository activityStreamRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.userTrainingEntryRepository = userTrainingEntryRepository;
        this.activityStreamRepository = activityStreamRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchNearbyUsers(double lat, double lon, double radiusKm, User currentUser) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
        if (radiusKm < 1) radiusKm = 1;
        if (radiusKm > 200) radiusKm = 200;
        double latDelta = radiusKm / 111.0;
        double cos = Math.cos(Math.toRadians(lat));
        double lonDelta = radiusKm / (111.0 * Math.max(cos, 0.0001));
        List<User> candidates = userRepository.findNearbyDiscoverableUsers(
                lat - latDelta, lat + latDelta,
                lon - lonDelta, lon + lonDelta,
                currentUser.getId());
        List<UserSearchResultDto> result = new ArrayList<>();
        for (User u : candidates) {
            double d = haversineKm(lat, lon, u.getLatitude(), u.getLongitude());
            if (d <= radiusKm) {
                UserSearchResultDto dto = toSearchDto(u, currentUser.getId());
                dto.setDistanceKm(Math.round(d * 10.0) / 10.0);
                result.add(dto);
            }
        }
        result.sort(Comparator.comparing(UserSearchResultDto::getDistanceKm,
                Comparator.nullsLast(Comparator.naturalOrder())));
        if (result.size() > 50) return result.subList(0, 50);
        return result;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchUsers(String query, User currentUser) {
        if (query == null || query.trim().length() < 2) return List.of();
        List<User> matches = userRepository.searchDiscoverableUsers(
                query.trim(), currentUser.getId(), PageRequest.of(0, 25));
        List<UserSearchResultDto> result = new ArrayList<>();
        for (User u : matches) {
            result.add(toSearchDto(u, currentUser.getId()));
        }
        return result;
    }

    public FriendshipDto sendRequest(User requester, Long addresseeId) {
        if (addresseeId == null) throw new IllegalArgumentException("Addressee id required");
        if (addresseeId.equals(requester.getId())) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }
        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!addressee.isDiscoverableByOthers()) {
            throw new IllegalStateException("This user is not discoverable");
        }
        if (friendshipRepository.existsBetween(requester.getId(), addressee.getId())) {
            throw new IllegalStateException("Friendship already exists");
        }
        Friendship friendship = new Friendship(requester, addressee);
        // Bots auto-accept friend requests so they act like lively community users.
        if (addressee.isBot()) {
            friendship.setStatus(FriendshipStatus.ACCEPTED);
        }
        friendship = friendshipRepository.save(friendship);
        return toFriendshipDto(friendship, requester.getId());
    }

    public FriendshipDto acceptRequest(Long friendshipId, User currentUser) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (!f.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the addressee can accept");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Friendship is not pending");
        }
        f.setStatus(FriendshipStatus.ACCEPTED);
        f.setRespondedAt(LocalDateTime.now());
        return toFriendshipDto(f, currentUser.getId());
    }

    public void declineRequest(Long friendshipId, User currentUser) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (!f.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the addressee can decline");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Friendship is not pending");
        }
        friendshipRepository.delete(f);
    }

    public void removeFriend(Long friendshipId, User currentUser) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        Long uid = currentUser.getId();
        if (!f.getRequester().getId().equals(uid) && !f.getAddressee().getId().equals(uid)) {
            throw new IllegalStateException("Not your friendship");
        }
        friendshipRepository.delete(f);
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> listFriends(User user) {
        return friendshipRepository.findAcceptedFriendships(user.getId()).stream()
                .map(f -> toFriendshipDto(f, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> listIncomingRequests(User user) {
        return friendshipRepository.findAllByAddresseeAndStatus(user, FriendshipStatus.PENDING).stream()
                .map(f -> toFriendshipDto(f, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> listOutgoingRequests(User user) {
        return friendshipRepository.findAllByRequesterAndStatus(user, FriendshipStatus.PENDING).stream()
                .map(f -> toFriendshipDto(f, user.getId()))
                .toList();
    }

    /**
     * Returns friends that have a training planned for today which is not yet marked as completed.
     * Pragmatic "Live Training Now" MVP: without a real-time tracking subsystem, we surface
     * today's pending planned trainings from accepted friends.
     */
    @Transactional(readOnly = true)
    public List<LiveTrainingFriendDto> getLiveTrainingFriends(User user) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(user.getId());
        LocalDate today = LocalDate.now();
        List<LiveTrainingFriendDto> result = new ArrayList<>();
        for (Friendship f : friendships) {
            User friend = f.getOtherUser(user.getId());
            List<UserTrainingEntry> entries = userTrainingEntryRepository
                    .findEntriesForUserByDate(friend.getId(), today);
            for (UserTrainingEntry e : entries) {
                if (Boolean.TRUE.equals(e.getCompleted())) continue;
                Training t = e.getTraining();
                result.add(new LiveTrainingFriendDto(
                        friend.getId(),
                        friend.getUsername(),
                        buildDisplayName(friend),
                        friend.getProfileImageFilename(),
                        t != null ? t.getName() : null,
                        t != null ? t.getTrainingType() : null,
                        t != null ? t.getDurationMinutes() : null,
                        t != null ? t.getWorkPace() : null
                ));
            }
        }
        if (result.size() > 20) return result.subList(0, 20);
        return result;
    }

    @Transactional(readOnly = true)
    public List<FriendActivityDto> getFriendsActivity(User user) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(user.getId());
        List<FriendActivityDto> activities = new ArrayList<>();
        for (Friendship f : friendships) {
            User friend = f.getOtherUser(user.getId());
            List<CompletedTraining> recent = completedTrainingRepository
                    .findByUserIdOrderByTrainingDateDescUploadDateDesc(friend.getId(), PageRequest.of(0, 5));
            for (CompletedTraining ct : recent) {
                activities.add(toActivityDto(ct, friend));
            }
        }
        activities.sort(Comparator.comparing(FriendActivityDto::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (activities.size() > 50) return activities.subList(0, 50);
        return activities;
    }

    // ---- mapping helpers ----

    private UserSearchResultDto toSearchDto(User u, Long currentUserId) {
        Optional<Friendship> existing = friendshipRepository.findBetween(currentUserId, u.getId());
        String status = "NONE";
        Long fid = null;
        if (existing.isPresent()) {
            Friendship f = existing.get();
            fid = f.getId();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                status = "FRIENDS";
            } else if (f.getStatus() == FriendshipStatus.PENDING) {
                status = f.getRequester().getId().equals(currentUserId) ? "PENDING_OUT" : "PENDING_IN";
            }
        }
        return new UserSearchResultDto(u.getId(), u.getUsername(), buildDisplayName(u),
                u.getProfileImageFilename(), status, fid);
    }

    private FriendshipDto toFriendshipDto(Friendship f, Long currentUserId) {
        FriendshipDto dto = new FriendshipDto();
        dto.setId(f.getId());
        dto.setStatus(f.getStatus().name());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setRespondedAt(f.getRespondedAt());
        User other = f.getOtherUser(currentUserId);
        UserSearchResultDto ou = new UserSearchResultDto(other.getId(), other.getUsername(),
                buildDisplayName(other), other.getProfileImageFilename(),
                f.getStatus() == FriendshipStatus.ACCEPTED ? "FRIENDS"
                        : (f.getRequester().getId().equals(currentUserId) ? "PENDING_OUT" : "PENDING_IN"),
                f.getId());
        dto.setOtherUser(ou);
        dto.setDirection(f.getRequester().getId().equals(currentUserId) ? "OUTGOING" : "INCOMING");
        return dto;
    }

    private FriendActivityDto toActivityDto(CompletedTraining ct, User friend) {
        FriendActivityDto dto = new FriendActivityDto();
        dto.setActivityId(ct.getId());
        dto.setFriendId(friend.getId());
        dto.setFriendUsername(friend.getUsername());
        dto.setFriendDisplayName(buildDisplayName(friend));
        dto.setProfileImageFilename(friend.getProfileImageFilename());
        dto.setActivityType("COMPLETED_TRAINING");
        dto.setTitle(ct.getSport() != null ? ct.getSport() : "Training");
        dto.setDate(ct.getTrainingDate());
        dto.setStartTime(ct.getStartTime());
        dto.setDistanceKm(ct.getDistanceKm());
        dto.setDurationSeconds(ct.getDurationSeconds());
        dto.setSport(ct.getSport());
        dto.setAveragePaceSecondsPerKm(ct.getAveragePaceSecondsPerKm());
        dto.setAverageHeartRate(ct.getAverageHeartRate());
        dto.setElevationGainM(ct.getElevationGainM());
        dto.setCalories(ct.getCalories());
        dto.setStartLatitude(ct.getStartLatitude());
        dto.setStartLongitude(ct.getStartLongitude());
        dto.setPreviewTrack(loadPreviewTrack(ct.getId()));
        return dto;
    }

    /**
     * Loads the GPS stream for a completed training and downsamples it to a handful of
     * points so the frontend can draw a route silhouette without shipping full resolution.
     */
    private double[][] loadPreviewTrack(Long completedTrainingId) {
        if (completedTrainingId == null) return null;
        Optional<ActivityStream> stream = activityStreamRepository.findByCompletedTrainingId(completedTrainingId);
        if (stream.isEmpty()) return null;
        double[][] full = parseGpsTrack(stream.get().getLatlngJson());
        if (full == null || full.length < 2) return null;
        return downsampleTrack(full, 60);
    }

    private double[][] parseGpsTrack(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode dataNode;
            if (root.isArray()) {
                dataNode = root;
            } else if (root.isObject() && root.has("data")) {
                dataNode = root.get("data");
            } else {
                return null;
            }
            int size = dataNode.size();
            if (size == 0) return null;
            double[][] result = new double[size][2];
            int idx = 0;
            for (JsonNode point : dataNode) {
                if (point.isArray() && point.size() >= 2
                        && !point.get(0).isNull() && !point.get(1).isNull()) {
                    result[idx][0] = point.get(0).doubleValue();
                    result[idx][1] = point.get(1).doubleValue();
                    idx++;
                }
            }
            if (idx == 0) return null;
            if (idx < size) {
                double[][] trimmed = new double[idx][2];
                System.arraycopy(result, 0, trimmed, 0, idx);
                return trimmed;
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private double[][] downsampleTrack(double[][] full, int maxPoints) {
        if (full == null || full.length == 0) return null;
        if (full.length <= maxPoints) return full;
        int step = (int) Math.ceil((double) full.length / maxPoints);
        List<double[]> reduced = new ArrayList<>(maxPoints + 1);
        for (int i = 0; i < full.length; i += step) {
            reduced.add(full[i]);
        }
        double[] last = full[full.length - 1];
        if (reduced.get(reduced.size() - 1) != last) {
            reduced.add(last);
        }
        return reduced.toArray(new double[0][]);
    }

    private String buildDisplayName(User u) {
        String first = u.getFirstName();
        String last = u.getLastName();
        if (first != null && !first.isBlank() && last != null && !last.isBlank()) {
            return first + " " + last;
        }
        if (first != null && !first.isBlank()) return first;
        if (last != null && !last.isBlank()) return last;
        return u.getUsername();
    }
}
