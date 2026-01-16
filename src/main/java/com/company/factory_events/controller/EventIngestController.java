package com.company.factory_events.controller;

import com.company.factory_events.dto.BatchIngestResponseDto;
import com.company.factory_events.dto.EventRequestDto;
import com.company.factory_events.service.EventIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventIngestController {

    private final EventIngestService eventIngestService;

    public EventIngestController(EventIngestService eventIngestService) {
        this.eventIngestService = eventIngestService;
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchIngestResponseDto> ingestBatch(
            @RequestBody List<EventRequestDto> events) {

        BatchIngestResponseDto response =
                eventIngestService.ingestBatch(events);

        return ResponseEntity.ok(response);
    }
}

