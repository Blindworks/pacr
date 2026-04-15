package com.trainingsplan.controller;

import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.TrendingTopicDto;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.AppNewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public (authenticated) read-only endpoints for the News Hub.
 */
@RestController
@RequestMapping("/api/news")
public class PublicNewsController {

    private final AppNewsService newsService;
    private final SecurityUtils securityUtils;

    public PublicNewsController(AppNewsService newsService, SecurityUtils securityUtils) {
        this.newsService = newsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<?> listPublished() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<AppNewsDto> dtos = newsService.findAllPublished().stream()
                .map(newsService::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/featured")
    public ResponseEntity<?> featured() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        AppNews featured = newsService.findFeatured();
        if (featured == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(newsService.toDto(featured));
    }

    @GetMapping("/trending")
    public ResponseEntity<?> trending() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<TrendingTopicDto> topics = newsService.trendingTopics();
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        AppNews news;
        try {
            news = newsService.findById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
        if (!news.isPublished()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(newsService.toDto(news));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        newsService.recordView(id, user);
        return ResponseEntity.noContent().build();
    }
}
