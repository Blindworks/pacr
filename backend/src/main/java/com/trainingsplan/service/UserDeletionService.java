package com.trainingsplan.service;

import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO-compliant user deletion. Removes all user-owned data from the database while preserving
 * {@code community_routes} (only the creator reference is nulled, so the route itself remains
 * available to the community).
 *
 * <p>Deletion is performed via native SQL inside a single transaction to guarantee that either
 * all user data is removed or none at all. The order of the DELETE statements respects all
 * existing foreign-key constraints.
 */
@Service
public class UserDeletionService {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionService.class);

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;

    public UserDeletionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Deletes the user with the given id and all owned data.
     *
     * @param userId          id of the user to delete
     * @param currentUserId   id of the admin performing the deletion (must differ from userId)
     * @param confirmUsername username the caller provided as a confirmation token; must match the
     *                        target user's username
     * @throws IllegalArgumentException if the user does not exist or the confirmation fails
     * @throws IllegalStateException    if the admin is trying to delete their own account
     */
    @Transactional
    public void deleteUserCompletely(Long userId, Long currentUserId, String confirmUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new IllegalStateException("Admins cannot delete their own account");
        }
        if (confirmUsername == null || !confirmUsername.equals(user.getUsername())) {
            throw new IllegalArgumentException("Username confirmation does not match");
        }

        log.warn("GDPR DELETE: removing user id={} username={}", userId, user.getUsername());

        // --- Group events owned (as trainer) by this user: cascade children, then events ---
        exec("DELETE FROM group_event_exceptions WHERE event_id IN " +
                "(SELECT id FROM group_events WHERE trainer_id = :uid)", userId);
        exec("DELETE FROM group_event_registrations WHERE event_id IN " +
                "(SELECT id FROM group_events WHERE trainer_id = :uid)", userId);
        exec("DELETE FROM group_events WHERE trainer_id = :uid", userId);

        // --- User's own registrations to other people's group events ---
        exec("DELETE FROM group_event_registrations WHERE user_id = :uid", userId);

        // --- Friendships in both directions ---
        exec("DELETE FROM friendships WHERE requester_id = :uid OR addressee_id = :uid", userId);

        // --- Plan adjustments (depend on user_training_entries) ---
        exec("DELETE FROM plan_adjustments WHERE user_id = :uid", userId);

        // --- Competitions are global (no owner) and are preserved. ---
        // --- User's own registrations (and their user_training_entries) ---
        exec("DELETE FROM user_training_entries WHERE competition_registration_id IN " +
                "(SELECT id FROM competition_registrations WHERE user_id = :uid)", userId);
        exec("DELETE FROM competition_registrations WHERE user_id = :uid", userId);

        // --- Personal records ---
        exec("DELETE FROM personal_record_entries WHERE personal_record_id IN " +
                "(SELECT id FROM personal_records WHERE user_id = :uid)", userId);
        exec("DELETE FROM personal_records WHERE user_id = :uid", userId);

        // --- Achievements ---
        exec("DELETE FROM user_achievements WHERE user_id = :uid", userId);

        // --- Route attempts ---
        exec("DELETE FROM route_attempts WHERE user_id = :uid", userId);

        // --- Community routes: preserve, but sever all ties to this user ---
        exec("UPDATE community_routes SET source_activity_id = NULL WHERE source_activity_id IN " +
                "(SELECT id FROM completed_trainings WHERE user_id = :uid)", userId);
        exec("UPDATE community_routes SET creator_id = NULL WHERE creator_id = :uid", userId);

        // --- Completed trainings (now safe, no community_routes FK left) ---
        exec("DELETE FROM completed_trainings WHERE user_id = :uid", userId);

        // --- AI training plans (chain: workout -> day -> plan) ---
        exec("DELETE FROM ai_training_workout WHERE day_id IN " +
                "(SELECT id FROM ai_training_day WHERE plan_id IN " +
                "(SELECT id FROM ai_training_plan WHERE user_id = :uid))", userId);
        exec("DELETE FROM ai_training_day WHERE plan_id IN " +
                "(SELECT id FROM ai_training_plan WHERE user_id = :uid)", userId);
        exec("DELETE FROM ai_training_plan WHERE user_id = :uid", userId);

        // --- Daily data ---
        exec("DELETE FROM daily_coach_sessions WHERE user_id = :uid", userId);
        exec("DELETE FROM daily_metrics WHERE user_id = :uid", userId);
        exec("DELETE FROM body_metrics WHERE user_id = :uid", userId);
        exec("DELETE FROM blood_pressure_measurements WHERE user_id = :uid", userId);
        exec("DELETE FROM body_measurements WHERE user_id = :uid", userId);
        exec("DELETE FROM sleep_data WHERE user_id = :uid", userId);
        exec("DELETE FROM asthma_entries WHERE user_id = :uid", userId);
        exec("DELETE FROM cycle_entries WHERE user_id = :uid", userId);
        exec("DELETE FROM cycle_settings WHERE user_id = :uid", userId);

        // --- Bot profile (for scheduled bot runner users) ---
        exec("DELETE FROM bot_profiles WHERE user_id = :uid", userId);

        // --- Feedback, notifications, news, login messages ---
        exec("DELETE FROM user_feedback WHERE user_id = :uid", userId);
        exec("DELETE FROM user_notification_preferences WHERE user_id = :uid", userId);
        exec("DELETE FROM app_news_sent_log WHERE user_id = :uid", userId);
        exec("DELETE FROM login_message_seen_log WHERE user_id = :uid", userId);

        // --- Integration tokens ---
        exec("DELETE FROM coros_token WHERE user_id = :uid", userId);
        exec("DELETE FROM strava_token WHERE user_id = :uid", userId);
        exec("DELETE FROM refresh_tokens WHERE user_id = :uid", userId);

        // --- Anonymize audit log (actor_id is a soft reference, no FK) ---
        exec("UPDATE audit_logs SET actor_id = NULL, actor_username = NULL WHERE actor_id = :uid",
                userId);

        em.flush();
        em.clear();

        // --- Finally, the user row itself ---
        exec("DELETE FROM users WHERE id = :uid", userId);

        log.warn("GDPR DELETE: user id={} removed successfully", userId);
    }

    private void exec(String sql, Long userId) {
        em.createNativeQuery(sql).setParameter("uid", userId).executeUpdate();
    }
}
