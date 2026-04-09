package com.trainingsplan.controller;

import com.trainingsplan.dto.BotCreateRequest;
import com.trainingsplan.dto.BotProfileDto;
import com.trainingsplan.service.BotRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bot-runners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBotRunnerController {

    private final BotRunnerService botRunnerService;

    public AdminBotRunnerController(BotRunnerService botRunnerService) {
        this.botRunnerService = botRunnerService;
    }

    @GetMapping
    public List<BotProfileDto> list() {
        return botRunnerService.listBots();
    }

    @GetMapping("/{id}")
    public BotProfileDto get(@PathVariable Long id) {
        return botRunnerService.getBot(id);
    }

    @PostMapping
    public ResponseEntity<BotProfileDto> create(@RequestBody BotCreateRequest req) {
        return ResponseEntity.ok(botRunnerService.createBot(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BotProfileDto> update(@PathVariable Long id, @RequestBody BotCreateRequest req) {
        return ResponseEntity.ok(botRunnerService.updateBot(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        botRunnerService.deleteBot(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run-now")
    public ResponseEntity<BotProfileDto> runNow(@PathVariable Long id) {
        return ResponseEntity.ok(botRunnerService.runBotNow(id));
    }
}
