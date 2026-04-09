package com.trainingsplan.controller;

import com.trainingsplan.dto.CommunityRouteDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.service.CommunityRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/community-routes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityRouteController {

    private final CommunityRouteService communityRouteService;
    private final UserRepository userRepository;

    public AdminCommunityRouteController(CommunityRouteService communityRouteService,
                                          UserRepository userRepository) {
        this.communityRouteService = communityRouteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CommunityRouteDto>> getAll() {
        return ResponseEntity.ok(communityRouteService.getAllRoutesForAdmin());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadGpx(@RequestParam("file") MultipartFile file,
                                        @RequestParam("name") String name,
                                        Principal principal) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".gpx")) {
            return ResponseEntity.badRequest().body("Only .gpx files are supported");
        }

        User admin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        try {
            CommunityRouteDto dto = communityRouteService.createFromGpx(admin, name, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to read file");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        try {
            communityRouteService.adminDeleteRoute(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
