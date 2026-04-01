package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.RouteAttemptDto;
import com.trainingsplan.dto.SelectRouteAttemptRequest;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.RouteAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-attempts")
@RequiresSubscription(SubscriptionPlan.PRO)
public class RouteAttemptController {

    private final RouteAttemptService routeAttemptService;
    private final SecurityUtils securityUtils;

    public RouteAttemptController(RouteAttemptService routeAttemptService,
                                   SecurityUtils securityUtils) {
        this.routeAttemptService = routeAttemptService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<?> selectRoute(@RequestBody SelectRouteAttemptRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            RouteAttemptDto dto = routeAttemptService.selectRoute(user, request.routeId());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/pending")
    public ResponseEntity<?> cancelPendingAttempt() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            routeAttemptService.cancelPendingAttempt(user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAttempt() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        RouteAttemptDto dto = routeAttemptService.getPendingAttempt(user);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAttemptResult(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        try {
            RouteAttemptDto dto = routeAttemptService.getAttemptResult(id, user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAttempts() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isCommunityRoutesEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Community routes not enabled");

        List<RouteAttemptDto> attempts = routeAttemptService.getMyAttempts(user);
        return ResponseEntity.ok(attempts);
    }
}
