package com.trainingsplan.controller;

import com.trainingsplan.dto.AppNewsCommentDto;
import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.AppNewsLikeDto;
import com.trainingsplan.dto.CreateActivityCommentRequest;
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
 * Public (authenticated) read-only + interaction endpoints for the News Hub.
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

    private boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
    }

    @GetMapping
    public ResponseEntity<?> listPublished() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<AppNewsDto> dtos = newsService.findAllPublished().stream()
                .map(n -> newsService.toDto(n, user))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/featured")
    public ResponseEntity<?> featured() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        AppNews featured = newsService.findFeatured();
        if (featured == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(newsService.toDto(featured, user));
    }

    @GetMapping("/trending")
    public ResponseEntity<?> trending() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<TrendingTopicDto> topics = newsService.trendingTopics();
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/trending-news")
    public ResponseEntity<?> trendingNews(@RequestParam(defaultValue = "3") int limit) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<AppNewsDto> dtos = newsService.getTrendingNews(limit).stream()
                .map(n -> newsService.toDto(n, user))
                .toList();
        return ResponseEntity.ok(dtos);
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
        return ResponseEntity.ok(newsService.toDto(news, user));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        newsService.recordView(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            AppNewsLikeDto dto = newsService.toggleLike(id, user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> listComments(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            List<AppNewsCommentDto> list = newsService.listComments(id, user, isAdmin(user));
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestBody CreateActivityCommentRequest body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            AppNewsCommentDto saved = newsService.addComment(id, body.content(), user);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            newsService.deleteComment(commentId, user, isAdmin(user));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
