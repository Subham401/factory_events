package com.company.factory_events.service;

import com.company.factory_events.dto.StatsResponseDto;
import com.company.factory_events.model.EventEntity;
import com.company.factory_events.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StatsService {

    private final EventRepository eventRepository;

    public StatsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public StatsResponseDto getStats(String machineId, Instant start, Instant end) {

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("INVALID_TIME_WINDOW");
        }

        List<EventEntity> events =
                eventRepository.findByMachineIdAndEventTimeGreaterThanEqualAndEventTimeLessThan(
                        machineId, start, end
                );

        long eventsCount = events.size();

        long defectsCount = events.stream()
                .filter(e -> e.getDefectCount() != -1)
                .mapToLong(EventEntity::getDefectCount)
                .sum();

        double windowHours = Duration.between(start, end).toSeconds() / 3600.0;

        double avgDefectRate =
                windowHours == 0 ? 0.0 : defectsCount / windowHours;

        String status =
                avgDefectRate < 2.0 ? "Healthy" : "Warning";

        StatsResponseDto response = new StatsResponseDto();
        response.setMachineId(machineId);
        response.setStart(start);
        response.setEnd(end);
        response.setEventsCount(eventsCount);
        response.setDefectsCount(defectsCount);
        response.setAvgDefectRate(avgDefectRate);
        response.setStatus(status);

        return response;
    }

}
