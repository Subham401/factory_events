package com.company.factory_events;

import com.company.factory_events.dto.EventRequestDto;
import com.company.factory_events.repository.EventRepository;
import com.company.factory_events.service.EventIngestService;
import com.company.factory_events.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DataCorruptionTest {

    @Autowired
    private EventIngestService ingestService;

    @Autowired
    private EventRepository eventRepository;

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
    @Transactional
    @Rollback
    void concurrentIngestionDoesNotCorruptData() throws Exception {

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Callable<Void> task = () -> {
            ingestService.ingestBatch(
                    List.of(validEvent("E-CONCURRENT"))
            );
            return null;
        };

        List<Callable<Void>> tasks = List.of(
                task, task, task, task, task,
                task, task, task, task, task
        );

        executor.invokeAll(tasks);
        executor.shutdown();

        assertEquals(1, eventRepository.count());
    }
}
