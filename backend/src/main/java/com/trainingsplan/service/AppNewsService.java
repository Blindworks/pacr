package com.trainingsplan.service;

import com.trainingsplan.dto.AppNewsCommentDto;
import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.AppNewsLikeDto;
import com.trainingsplan.dto.CreateAppNewsRequest;
import com.trainingsplan.dto.TrendingTopicDto;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.AppNewsComment;
import com.trainingsplan.entity.AppNewsLike;
import com.trainingsplan.entity.AppNewsSentLog;
import com.trainingsplan.entity.AppNewsView;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
import com.trainingsplan.repository.AppNewsCommentRepository;
import com.trainingsplan.repository.AppNewsLikeRepository;
import com.trainingsplan.repository.AppNewsRepository;
import com.trainingsplan.repository.AppNewsSentLogRepository;
import com.trainingsplan.repository.AppNewsViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppNewsService {

    private static final Logger log = LoggerFactory.getLogger(AppNewsService.class);

    private static final int TRENDING_WINDOW_DAYS = 7;
    private static final long TRENDING_BADGE_MIN_VIEWS = 10L;
    private static final int COMMENT_MAX_LENGTH = 2000;

    private final AppNewsRepository newsRepo;
    private final AppNewsSentLogRepository sentLogRepo;
    private final AppNewsViewRepository viewRepo;
    private final AppNewsLikeRepository likeRepo;
    private final AppNewsCommentRepository commentRepo;
    private final UserNotificationPreferencesService notifPrefsService;
    private final EmailService emailService;

    public AppNewsService(AppNewsRepository newsRepo, AppNewsSentLogRepository sentLogRepo,
                          AppNewsViewRepository viewRepo,
                          AppNewsLikeRepository likeRepo,
                          AppNewsCommentRepository commentRepo,
                          UserNotificationPreferencesService notifPrefsService, EmailService emailService) {
        this.newsRepo = newsRepo;
        this.sentLogRepo = sentLogRepo;
        this.viewRepo = viewRepo;
        this.likeRepo = likeRepo;
        this.commentRepo = commentRepo;
        this.notifPrefsService = notifPrefsService;
        this.emailService = emailService;
    }

    public List<AppNews> findAll() {
        return newsRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<AppNews> findAllPublished() {
        return newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc();
    }

    /**
     * Returns published news filtered by the given language codes.
     * News with {@code language == null} (legacy / manually created) are always included.
     */
    public List<AppNews> findAllPublishedForLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc();
        }
        return newsRepo.findPublishedForLanguages(languages);
    }

    /**
     * Creates a news entry imported from an external RSS feed. Idempotent:
     * if a news item with the given GUID already exists, returns {@code null}.
     */
    @Transactional
    public AppNews createFromExternal(com.trainingsplan.entity.ExternalNewsSource source,
                                      com.trainingsplan.service.ParsedFeedItem item,
                                      User systemUser) {
        if (item.guid() == null || item.guid().isBlank()) return null;
        if (newsRepo.existsByExternalGuid(item.guid())) return null;
        AppNews news = new AppNews();
        news.setTitle(item.title());
        // content is NOT NULL in the schema — fall back to excerpt or title.
        String content = (item.excerpt() != null && !item.excerpt().isBlank())
                ? item.excerpt() : item.title();
        news.setContent(content);
        news.setExcerpt(item.excerpt());
        news.setTopicTag(source.getName());
        news.setFeatured(false);
        news.setPublished(false);
        news.setCreatedBy(systemUser);
        news.setCreatedAt(LocalDateTime.now());
        news.setExternalGuid(item.guid());
        news.setExternalUrl(item.url());
        news.setExternalImageUrl(item.imageUrl());
        news.setLanguage(source.getLanguage());
        news.setExternalSource(source);
        return newsRepo.save(news);
    }

    public AppNews findById(Long id) {
        return newsRepo.findById(id).orElseThrow(() -> new RuntimeException("News not found: " + id));
    }

    /**
     * Returns the single featured, published news item (most recent by publishedAt) or null.
     */
    public AppNews findFeatured() {
        return newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc().stream()
                .filter(AppNews::isFeatured)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public AppNews create(CreateAppNewsRequest request, User creator) {
        AppNews news = new AppNews();
        applyRequest(news, request);
        news.setCreatedBy(creator);
        news.setCreatedAt(LocalDateTime.now());
        news.setPublished(false);
        return newsRepo.save(news);
    }

    @Transactional
    public AppNews update(Long id, CreateAppNewsRequest request) {
        AppNews news = newsRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found: " + id));
        applyRequest(news, request);
        return newsRepo.save(news);
    }

    private void applyRequest(AppNews news, CreateAppNewsRequest request) {
        news.setTitle(request.title());
        news.setContent(request.content());
        news.setExcerpt(request.excerpt());
        news.setTopicTag(request.topicTag());
        news.setHeroImageFilename(request.heroImageFilename());
        news.setFeatured(Boolean.TRUE.equals(request.isFeatured()));
    }

    @Transactional
    public AppNews publish(Long id) {
        AppNews news = newsRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found: " + id));
        news.setPublished(true);
        news.setPublishedAt(LocalDateTime.now());
        news = newsRepo.save(news);
        sendToSubscribers(news);
        return news;
    }

    @Transactional
    public void delete(Long id) {
        newsRepo.deleteById(id);
    }

    /**
     * Idempotent: records a view exactly once per (news, user).
     */
    @Transactional
    public void recordView(Long newsId, User user) {
        if (newsId == null || user == null) return;
        if (viewRepo.existsByAppNews_IdAndUser_Id(newsId, user.getId())) return;
        AppNews news = newsRepo.findById(newsId).orElse(null);
        if (news == null || !news.isPublished()) return;
        try {
            viewRepo.save(new AppNewsView(news, user));
        } catch (Exception e) {
            // Race-condition on unique constraint — safe to ignore.
            log.debug("Duplicate news view ignored for news={} user={}", newsId, user.getId());
        }
    }

    /**
     * Idempotently toggles like: if user already liked, removes it; otherwise creates it.
     */
    @Transactional
    public AppNewsLikeDto toggleLike(Long newsId, User user) {
        AppNews news = newsRepo.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found"));
        if (!news.isPublished()) throw new IllegalArgumentException("News not published");
        var existing = likeRepo.findByAppNews_IdAndUser_Id(newsId, user.getId());
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
        } else {
            likeRepo.save(new AppNewsLike(news, user));
        }
        long count = likeRepo.countByAppNews_Id(newsId);
        boolean has = likeRepo.existsByAppNews_IdAndUser_Id(newsId, user.getId());
        return new AppNewsLikeDto(count, has);
    }

    @Transactional(readOnly = true)
    public List<AppNewsCommentDto> listComments(Long newsId, User currentUser, boolean isAdmin) {
        AppNews news = newsRepo.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found"));
        if (!news.isPublished()) throw new IllegalArgumentException("News not published");
        List<AppNewsComment> comments = commentRepo.findByAppNews_IdOrderByCreatedAtAsc(newsId);
        return comments.stream().map(c -> toCommentDto(c, currentUser, isAdmin)).toList();
    }

    @Transactional
    public AppNewsCommentDto addComment(Long newsId, String content, User user) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content required");
        }
        if (content.length() > COMMENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Comment too long");
        }
        AppNews news = newsRepo.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found"));
        if (!news.isPublished()) throw new IllegalArgumentException("News not published");
        AppNewsComment saved = commentRepo.save(new AppNewsComment(news, user, content.trim()));
        return toCommentDto(saved, user, false);
    }

    @Transactional
    public void deleteComment(Long commentId, User user, boolean isAdmin) {
        AppNewsComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        boolean isAuthor = c.getUser() != null && c.getUser().getId().equals(user.getId());
        if (!isAuthor && !isAdmin) {
            throw new IllegalStateException("Not allowed to delete this comment");
        }
        commentRepo.delete(c);
    }

    /**
     * Returns the top-N trending published news by a weighted score
     * (views + likes*3 + comments*5) over the last TRENDING_WINDOW_DAYS days.
     */
    @Transactional(readOnly = true)
    public List<AppNews> getTrendingNews(int limit) {
        LocalDateTime from = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        List<AppNews> published = newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc();
        return published.stream()
                .map(n -> Map.entry(n, computeTrendingScore(n.getId(), from)))
                .filter(e -> e.getValue() > 0L)
                .sorted(Map.Entry.<AppNews, Long>comparingByValue().reversed())
                .limit(Math.max(1, limit))
                .map(Map.Entry::getKey)
                .toList();
    }

    private long computeTrendingScore(Long newsId, LocalDateTime from) {
        long views = viewRepo.countByAppNews_IdAndViewedAtAfter(newsId, from);
        long likes = likeRepo.countByAppNews_IdAndCreatedAtAfter(newsId, from);
        long comments = commentRepo.countByAppNews_IdAndCreatedAtAfter(newsId, from);
        return views + likes * 3L + comments * 5L;
    }

    private boolean isTrending(Long newsId) {
        LocalDateTime from = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        long views = viewRepo.countByAppNews_IdAndViewedAtAfter(newsId, from);
        return views >= TRENDING_BADGE_MIN_VIEWS;
    }

    /**
     * Aggregates top trending topicTags from published news of the last 30 days.
     * Returns up to 5 topics sorted by view count, with the most recent news headline per tag.
     */
    public List<TrendingTopicDto> trendingTopics() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        List<Object[]> rows = viewRepo.aggregateTrendingTags(from);
        Map<String, String> headlineByTag = new HashMap<>();
        for (AppNews n : newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc()) {
            if (n.getTopicTag() == null || n.getTopicTag().isBlank()) continue;
            headlineByTag.putIfAbsent(n.getTopicTag(), n.getTitle());
        }
        List<TrendingTopicDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            String tag = (String) r[0];
            long views = ((Number) r[1]).longValue();
            long newsCount = ((Number) r[2]).longValue();
            result.add(new TrendingTopicDto(tag, views, newsCount, headlineByTag.get(tag)));
        }
        // Secondary sort by newsCount desc, then alphabetical
        result.sort(Comparator
                .comparingLong(TrendingTopicDto::viewCount).reversed()
                .thenComparing(Comparator.comparingLong(TrendingTopicDto::newsCount).reversed())
                .thenComparing(TrendingTopicDto::tag));
        if (result.size() > 5) return result.subList(0, 5);
        return result;
    }

    private void sendToSubscribers(AppNews news) {
        List<UserNotificationPreferences> subscribers = notifPrefsService.findAllNewsSubscribers();
        for (UserNotificationPreferences prefs : subscribers) {
            User user = prefs.getUser();
            if (sentLogRepo.existsByUserIdAndNewsId(user.getId(), news.getId())) continue;
            try {
                emailService.sendSimpleMessage(
                    user.getEmail(),
                    "Neue App-Neuigkeit: " + news.getTitle(),
                    news.getContent()
                );
                AppNewsSentLog sentLog = new AppNewsSentLog();
                sentLog.setUser(user);
                sentLog.setNews(news);
                sentLog.setSentAt(LocalDateTime.now());
                sentLogRepo.save(sentLog);
            } catch (Exception e) {
                log.error("Failed to send news email to user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /**
     * Used by admin views — no hasLiked context.
     */
    public AppNewsDto toDto(AppNews news) {
        return toDto(news, null);
    }

    /**
     * User-aware DTO — populates counts and hasLiked flag for the given user.
     */
    public AppNewsDto toDto(AppNews news, User currentUser) {
        long viewCount = viewRepo.countByAppNews_Id(news.getId());
        long likeCount = likeRepo.countByAppNews_Id(news.getId());
        long commentCount = commentRepo.countByAppNews_Id(news.getId());
        boolean hasLiked = currentUser != null
                && likeRepo.existsByAppNews_IdAndUser_Id(news.getId(), currentUser.getId());
        boolean trending = isTrending(news.getId());
        String sourceName = news.getExternalSource() != null ? news.getExternalSource().getName() : null;
        return new AppNewsDto(
            news.getId(),
            news.getTitle(),
            news.getContent(),
            news.getExcerpt(),
            news.getTopicTag(),
            news.getHeroImageFilename(),
            news.isFeatured(),
            news.isPublished(),
            news.getPublishedAt(),
            news.getCreatedAt(),
            viewCount,
            likeCount,
            commentCount,
            hasLiked,
            trending,
            news.getExternalUrl(),
            news.getExternalImageUrl(),
            sourceName,
            news.getLanguage()
        );
    }

    private AppNewsCommentDto toCommentDto(AppNewsComment c, User currentUser, boolean isAdmin) {
        User u = c.getUser();
        boolean canDelete = (u != null && currentUser != null && u.getId().equals(currentUser.getId()))
                || isAdmin;
        return new AppNewsCommentDto(
                c.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                u != null ? buildDisplayName(u) : null,
                u != null ? u.getProfileImageFilename() : null,
                c.getContent(),
                c.getCreatedAt(),
                canDelete
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
