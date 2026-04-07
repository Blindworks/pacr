package com.trainingsplan.service;

import com.trainingsplan.dto.FriendActivityDto;
import com.trainingsplan.dto.FriendshipDto;
import com.trainingsplan.dto.UserSearchResultDto;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.Friendship;
import com.trainingsplan.entity.FriendshipStatus;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.FriendshipRepository;
import com.trainingsplan.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public FriendshipService(FriendshipRepository friendshipRepository,
                              UserRepository userRepository,
                              CompletedTrainingRepository completedTrainingRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.completedTrainingRepository = completedTrainingRepository;
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
        dto.setFriendId(friend.getId());
        dto.setFriendUsername(friend.getUsername());
        dto.setFriendDisplayName(buildDisplayName(friend));
        dto.setProfileImageFilename(friend.getProfileImageFilename());
        dto.setActivityType("COMPLETED_TRAINING");
        dto.setTitle(ct.getSport() != null ? ct.getSport() : "Training");
        dto.setDate(ct.getTrainingDate());
        dto.setDistanceKm(ct.getDistanceKm());
        dto.setDurationSeconds(ct.getDurationSeconds());
        dto.setSport(ct.getSport());
        return dto;
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
