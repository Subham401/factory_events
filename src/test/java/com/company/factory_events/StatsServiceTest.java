package com.company.factory_events;

import com.company.factory_events.dto.EventRequestDto;
import com.company.factory_events.service.EventIngestService;
import com.company.factory_events.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StatsServiceTest {

    @Autowired
    private EventIngestService ingestService;

    @Autowired
    private StatsService statsService;

    private EventRequestDto validEvent(String id) {
        EventRequestDto dto = new EventRequestDto();
        dto.setEventId(id);
        dto.setMachineId("M-001");
        dto.setEventTime(Instant.now().minusSeconds(60));
        dto.setDurationMs(1000);
        dto.setDefectCount(1);
        dto.setFactoryId("F01");
        dto.setLineId("L01");
        return dto;
    }


    @Test
    void defectMinusOneIgnoredInStats() {

        EventRequestDto e1 = new EventRequestDto();
        e1.setEventId("E-6");
        e1.setMachineId("M-001");
        e1.setEventTime(Instant.now().minusSeconds(100));
        e1.setDurationMs(1000);
        e1.setDefectCount(-1);

        ingestService.ingestBatch(List.of(e1));

        var stats = statsService.getStats(
                "M-001",
                Instant.now().minusSeconds(200),
                Instant.now()
        );

        assertEquals(0, stats.getDefectsCount());
    }

    @Test
    void startInclusiveEndExclusive() {

        Instant now = Instant.now();

        EventRequestDto event = validEvent("E-7");
        event.setEventTime(now);

        ingestService.ingestBatch(List.of(event));

        var stats = statsService.getStats(
                "M-001",
                now,
                now.plusSeconds(1)
        );

        assertEquals(1, stats.getEventsCount());
    }

}

