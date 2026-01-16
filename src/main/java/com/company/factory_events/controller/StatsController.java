package com.company.factory_events.controller;

import com.company.factory_events.dto.StatsResponseDto;
import com.company.factory_events.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<StatsResponseDto> getStats(
            @RequestParam String machineId,
            @RequestParam Instant start,
            @RequestParam Instant end) {

        StatsResponseDto response =
                statsService.getStats(machineId, start, end);

        return ResponseEntity.ok(response);
    }
}

