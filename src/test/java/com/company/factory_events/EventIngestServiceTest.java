package com.company.factory_events;

import com.company.factory_events.dto.EventRequestDto;
import com.company.factory_events.repository.EventRepository;
import com.company.factory_events.service.EventIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventIngestServiceTest {

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
    void identicalDuplicateEventIsDeduped() {

        EventRequestDto event = validEvent("E-1");

        ingestService.ingestBatch(List.of(event));
        ingestService.ingestBatch(List.of(event));

        assertEquals(1, eventRepository.count());
    }

    @Test
    void newerEventUpdatesExisting() throws InterruptedException {

        EventRequestDto original = validEvent("E-2");
        ingestService.ingestBatch(List.of(original));

        Thread.sleep(5); // ensure newer receivedTime

        EventRequestDto updated = validEvent("E-2");
        updated.setDefectCount(5);

        ingestService.ingestBatch(List.of(updated));

        var stored = eventRepository.findById("E-2").orElseThrow();
        assertEquals(5, stored.getDefectCount());
    }

    @Test
    void olderEventIsIgnored() {

        EventRequestDto first = validEvent("E-3");
        ingestService.ingestBatch(List.of(first));

        EventRequestDto older = validEvent("E-3");
        older.setDefectCount(10);

        ingestService.ingestBatch(List.of(older));

        var stored = eventRepository.findById("E-3").orElseThrow();
        assertEquals(1, stored.getDefectCount());
    }

    @Test
    void invalidDurationIsRejected() {

        EventRequestDto event = validEvent("E-4");
        event.setDurationMs(-5);

        ingestService.ingestBatch(List.of(event));

        assertEquals(0, eventRepository.count());
    }

    @Test
    void futureEventTimeRejected() {

        EventRequestDto event = validEvent("E-5");
        event.setEventTime(Instant.now().plusSeconds(20 * 60));

        ingestService.ingestBatch(List.of(event));

        assertEquals(0, eventRepository.count());
    }

    @Test
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
