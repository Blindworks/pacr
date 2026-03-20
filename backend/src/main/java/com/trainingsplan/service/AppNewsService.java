package com.trainingsplan.service;

import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.CreateAppNewsRequest;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.AppNewsSentLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
import com.trainingsplan.repository.AppNewsRepository;
import com.trainingsplan.repository.AppNewsSentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppNewsService {

    private static final Logger log = LoggerFactory.getLogger(AppNewsService.class);

    private final AppNewsRepository newsRepo;
    private final AppNewsSentLogRepository sentLogRepo;
    private final UserNotificationPreferencesService notifPrefsService;
    private final EmailService emailService;

    public AppNewsService(AppNewsRepository newsRepo, AppNewsSentLogRepository sentLogRepo,
                          UserNotificationPreferencesService notifPrefsService, EmailService emailService) {
        this.newsRepo = newsRepo;
        this.sentLogRepo = sentLogRepo;
        this.notifPrefsService = notifPrefsService;
        this.emailService = emailService;
    }

    public List<AppNews> findAll() {
        return newsRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public AppNews create(CreateAppNewsRequest request, User creator) {
        AppNews news = new AppNews();
        news.setTitle(request.title());
        news.setContent(request.content());
        news.setCreatedBy(creator);
        news.setCreatedAt(LocalDateTime.now());
        news.setPublished(false);
        return newsRepo.save(news);
    }

    @Transactional
    public AppNews update(Long id, CreateAppNewsRequest request) {
        AppNews news = newsRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found: " + id));
        news.setTitle(request.title());
        news.setContent(request.content());
        return newsRepo.save(news);
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
            news.isPublished(),
            news.getPublishedAt(),
            news.getCreatedAt()
        );
    }
}
