package com.trainingsplan.controller;

import com.trainingsplan.dto.AppNewsDto;
import com.trainingsplan.dto.CreateAppNewsRequest;
import com.trainingsplan.entity.AppNews;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.service.AppNewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/news")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {

    private final AppNewsService newsService;
    private final UserRepository userRepository;

    public AdminNewsController(AppNewsService newsService, UserRepository userRepository) {
        this.newsService = newsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AppNewsDto> getAll() {
        return newsService.findAll().stream().map(newsService::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<AppNewsDto> create(@RequestBody CreateAppNewsRequest request, Principal principal) {
        User creator = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        AppNews news = newsService.create(request, creator);
        return ResponseEntity.ok(newsService.toDto(news));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppNewsDto> update(@PathVariable Long id, @RequestBody CreateAppNewsRequest request) {
        AppNews news = newsService.update(id, request);
        return ResponseEntity.ok(newsService.toDto(news));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AppNewsDto> publish(@PathVariable Long id) {
        AppNews news = newsService.publish(id);
        return ResponseEntity.ok(newsService.toDto(news));
    }
}
