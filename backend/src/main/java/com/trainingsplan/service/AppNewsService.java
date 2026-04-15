package com.trainingsplan.service;

import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.CreateAppNewsRequest;
import com.trainingsplan.dto.TrendingTopicDto;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.AppNewsSentLog;
import com.trainingsplan.entity.AppNewsView;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
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

    private final AppNewsRepository newsRepo;
    private final AppNewsSentLogRepository sentLogRepo;
    private final AppNewsViewRepository viewRepo;
    private final UserNotificationPreferencesService notifPrefsService;
    private final EmailService emailService;

    public AppNewsService(AppNewsRepository newsRepo, AppNewsSentLogRepository sentLogRepo,
                          AppNewsViewRepository viewRepo,
                          UserNotificationPreferencesService notifPrefsService, EmailService emailService) {
        this.newsRepo = newsRepo;
        this.sentLogRepo = sentLogRepo;
        this.viewRepo = viewRepo;
        this.notifPrefsService = notifPrefsService;
        this.emailService = emailService;
    }

    public List<AppNews> findAll() {
        return newsRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<AppNews> findAllPublished() {
        return newsRepo.findAllByIsPublishedTrueOrderByPublishedAtDesc();
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

    public AppNewsDto toDto(AppNews news) {
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
            news.getCreatedAt()
        );
    }
}
