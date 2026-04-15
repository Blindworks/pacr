package com.trainingsplan.service;

import com.trainingsplan.dto.ActivityCommentDto;
import com.trainingsplan.dto.ActivityKudosDto;
import com.trainingsplan.entity.ActivityComment;
import com.trainingsplan.entity.ActivityKudos;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityCommentRepository;
import com.trainingsplan.repository.ActivityKudosRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.FriendshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivitySocialService {

    private final CompletedTrainingRepository completedTrainingRepository;
    private final ActivityKudosRepository kudosRepository;
    private final ActivityCommentRepository commentRepository;
    private final FriendshipRepository friendshipRepository;

    public ActivitySocialService(CompletedTrainingRepository completedTrainingRepository,
                                 ActivityKudosRepository kudosRepository,
                                 ActivityCommentRepository commentRepository,
                                 FriendshipRepository friendshipRepository) {
        this.completedTrainingRepository = completedTrainingRepository;
        this.kudosRepository = kudosRepository;
        this.commentRepository = commentRepository;
        this.friendshipRepository = friendshipRepository;
    }

    /**
     * Returns the CompletedTraining if the given user is allowed to see/interact with it:
     * either the owner of the activity, or an accepted friend of the owner.
     */
    private CompletedTraining requireVisible(Long completedTrainingId, User user) {
        CompletedTraining ct = completedTrainingRepository.findById(completedTrainingId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        User owner = ct.getUser();
        if (owner == null) throw new IllegalStateException("Activity has no owner");
        if (owner.getId().equals(user.getId())) return ct;
        if (friendshipRepository.areAcceptedFriends(owner.getId(), user.getId())) return ct;
        throw new IllegalStateException("Not allowed");
    }

    @Transactional(readOnly = true)
    public ActivityKudosDto getKudos(Long completedTrainingId, User user) {
        requireVisible(completedTrainingId, user);
        long count = kudosRepository.countByCompletedTraining_Id(completedTrainingId);
        boolean has = kudosRepository.existsByCompletedTraining_IdAndUser_Id(completedTrainingId, user.getId());
        return new ActivityKudosDto(count, has);
    }

    /**
     * Idempotently toggles kudos: if user already kudoed, removes it; otherwise creates it.
     * Returns the new state.
     */
    @Transactional
    public ActivityKudosDto toggleKudos(Long completedTrainingId, User user) {
        CompletedTraining ct = requireVisible(completedTrainingId, user);
        var existing = kudosRepository.findByCompletedTraining_IdAndUser_Id(completedTrainingId, user.getId());
        if (existing.isPresent()) {
            kudosRepository.delete(existing.get());
        } else {
            kudosRepository.save(new ActivityKudos(ct, user));
        }
        long count = kudosRepository.countByCompletedTraining_Id(completedTrainingId);
        boolean has = kudosRepository.existsByCompletedTraining_IdAndUser_Id(completedTrainingId, user.getId());
        return new ActivityKudosDto(count, has);
    }

    @Transactional(readOnly = true)
    public List<ActivityCommentDto> listComments(Long completedTrainingId, User user) {
        requireVisible(completedTrainingId, user);
        List<ActivityComment> comments = commentRepository.findByCompletedTraining_IdOrderByCreatedAtAsc(completedTrainingId);
        return comments.stream().map(this::toDto).toList();
    }

    @Transactional
    public ActivityCommentDto addComment(Long completedTrainingId, String content, User user) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content required");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment too long");
        }
        CompletedTraining ct = requireVisible(completedTrainingId, user);
        ActivityComment saved = commentRepository.save(new ActivityComment(ct, user, content.trim()));
        return toDto(saved);
    }

    @Transactional
    public void deleteComment(Long completedTrainingId, Long commentId, User user, boolean isAdmin) {
        requireVisible(completedTrainingId, user);
        ActivityComment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!c.getCompletedTraining().getId().equals(completedTrainingId)) {
            throw new IllegalArgumentException("Comment does not belong to this activity");
        }
        boolean isAuthor = c.getUser() != null && c.getUser().getId().equals(user.getId());
        if (!isAuthor && !isAdmin) {
            throw new IllegalStateException("Not allowed to delete this comment");
        }
        commentRepository.delete(c);
    }

    public long commentCount(Long completedTrainingId) {
        return commentRepository.countByCompletedTraining_Id(completedTrainingId);
    }

    public long kudosCount(Long completedTrainingId) {
        return kudosRepository.countByCompletedTraining_Id(completedTrainingId);
    }

    private ActivityCommentDto toDto(ActivityComment c) {
        User u = c.getUser();
        return new ActivityCommentDto(
                c.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                u != null ? buildDisplayName(u) : null,
                u != null ? u.getProfileImageFilename() : null,
                c.getContent(),
                c.getCreatedAt()
        );
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
