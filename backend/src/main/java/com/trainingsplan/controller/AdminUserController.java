package com.trainingsplan.controller;

import com.trainingsplan.dto.UserSummaryDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public List<UserSummaryDto> search(@RequestParam("q") String query,
                                       @RequestParam(value = "limit", defaultValue = "20") int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<User> users = userRepository.searchByUsernameOrEmail(query.trim(), pageable);
        return users.stream()
            .map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail()))
            .toList();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<UserSummaryDto> getSummary(@PathVariable("id") Long id) {
        return userRepository.findById(id)
            .map(u -> ResponseEntity.ok(new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
