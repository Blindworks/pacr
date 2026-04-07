package com.trainingsplan.controller;

import com.trainingsplan.dto.FriendActivityDto;
import com.trainingsplan.dto.FriendshipDto;
import com.trainingsplan.dto.UserSearchResultDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.FriendshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final SecurityUtils securityUtils;

    public FriendshipController(FriendshipService friendshipService, SecurityUtils securityUtils) {
        this.friendshipService = friendshipService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String query) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<UserSearchResultDto> results = friendshipService.searchUsers(query, user);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/nearby")
    public ResponseEntity<?> searchNearby(@RequestParam("lat") double lat,
                                           @RequestParam("lon") double lon,
                                           @RequestParam(value = "radiusKm", defaultValue = "25") double radiusKm) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(friendshipService.searchNearbyUsers(lat, lon, radiusKm, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listFriends() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(friendshipService.listFriends(user));
    }

    @GetMapping("/incoming")
    public ResponseEntity<?> listIncoming() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(friendshipService.listIncomingRequests(user));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<?> listOutgoing() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(friendshipService.listOutgoingRequests(user));
    }

    @PostMapping
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Object> body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Object idObj = body.get("addresseeId");
        if (idObj == null) return ResponseEntity.badRequest().body("addresseeId required");
        Long addresseeId;
        try {
            addresseeId = Long.valueOf(idObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid addresseeId");
        }
        try {
            FriendshipDto dto = friendshipService.sendRequest(user, addresseeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(friendshipService.acceptRequest(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<?> decline(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            friendshipService.declineRequest(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            friendshipService.removeFriend(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/activity")
    public ResponseEntity<?> activity() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<FriendActivityDto> activities = friendshipService.getFriendsActivity(user);
        return ResponseEntity.ok(activities);
    }
}
