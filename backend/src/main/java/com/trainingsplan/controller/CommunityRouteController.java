package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.*;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.CommunityRouteService;
import com.trainingsplan.service.LeaderboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community-routes")
@RequiresSubscription(SubscriptionPlan.PRO)
public class CommunityRouteController {

    private final CommunityRouteService communityRouteService;
    private final LeaderboardService leaderboardService;
    private final SecurityUtils securityUtils;

    public CommunityRouteController(CommunityRouteService communityRouteService,
                                     LeaderboardService leaderboardService,
                                     SecurityUtils securityUtils) {
        this.communityRouteService = communityRouteService;
        this.leaderboardService = leaderboardService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<?> shareRoute(@RequestBody CreateCommunityRouteRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            CommunityRouteDto dto = communityRouteService.shareRoute(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> unshareRoute(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            communityRouteService.unshareRoute(user, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoute(@PathVariable Long id,
                                          @RequestBody UpdateCommunityRouteRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            CommunityRouteDto dto = communityRouteService.updateRoute(user, id, request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyRoutes(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "distance") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        List<CommunityRouteDto> routes = communityRouteService.findNearbyRoutes(lat, lon, radiusKm, sortBy, page, size);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyRoutes() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        List<CommunityRouteDto> routes = communityRouteService.getMyRoutes(user);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRouteDetail(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            CommunityRouteDetailDto dto = communityRouteService.getRouteDetail(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable Long id,
                                             @RequestParam(defaultValue = "ALL_TIME") String period) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        List<LeaderboardEntryDto> entries = leaderboardService.getLeaderboard(id, period);
        return ResponseEntity.ok(entries);
    }
}
