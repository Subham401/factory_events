package com.company.factory_events.repository;

import com.company.factory_events.model.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity,String> {
    List<EventEntity> findByMachineIdAndEventTimeGreaterThanEqualAndEventTimeLessThan(
            String machineId,
            Instant start,
            Instant end
    );
}
