package com.company.factory_events.service;


import com.company.factory_events.dto.BatchIngestResponseDto;
import com.company.factory_events.dto.EventRequestDto;
import com.company.factory_events.model.EventEntity;
import com.company.factory_events.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EventIngestService {

    private final EventRepository eventRepository;

    public EventIngestService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public BatchIngestResponseDto ingestBatch(List<EventRequestDto> events) {

        BatchIngestResponseDto response = new BatchIngestResponseDto();

        for (EventRequestDto dto : events) {
            try {
                IngestResult result = ingestSingle(dto);

                switch (result) {
                    case ACCEPTED -> response.setAccepted(response.getAccepted() + 1);
                    case UPDATED -> response.setUpdated(response.getUpdated() + 1);
                    case DEDUPED -> response.setDeduped(response.getDeduped() + 1);
                    case IGNORED -> response.setIgnored(response.getDeduped() + 1);
                }

            } catch (IllegalArgumentException ex) {
                response.setRejected(response.getRejected() + 1);
                response.getRejections()
                        .add(new BatchIngestResponseDto.Rejection(
                                dto.getEventId(),
                                ex.getMessage()
                        ));
            }
        }

        return response;
    }


    @Transactional
    protected IngestResult ingestSingle(EventRequestDto dto) {

        validate(dto);

        Instant receivedTime = dto.getReceivedTime() != null
                ? dto.getReceivedTime()
                : Instant.now();

        String payloadHash = computePayloadHash(dto);

        return eventRepository.findById(dto.getEventId())
                .map(existing -> {

                    if (existing.getPayloadHash().equals(payloadHash)) {
                        return IngestResult.DEDUPED;
                    }

                    if (receivedTime.isAfter(existing.getReceivedTime())) {
                        existing.setMachineId(dto.getMachineId());
                        existing.setEventTime(dto.getEventTime());
                        existing.setReceivedTime(receivedTime);
                        existing.setDurationMs(dto.getDurationMs());
                        existing.setDefectCount(dto.getDefectCount());
                        existing.setPayloadHash(payloadHash);
                        existing.setFactoryId(dto.getFactoryId());
                        existing.setLineId(dto.getLineId());

                        eventRepository.save(existing);
                        return IngestResult.UPDATED;
                    }

                    return IngestResult.IGNORED;

                })
                .orElseGet(() -> {
                    EventEntity entity = mapToEntity(dto, receivedTime, payloadHash);
                    eventRepository.save(entity);
                    return IngestResult.ACCEPTED;
                });
    }


    protected EventEntity mapToEntity(EventRequestDto dto,
                                      Instant receivedTime,
                                      String payloadHash) {

        EventEntity entity = new EventEntity();
        entity.setEventId(dto.getEventId());
        entity.setMachineId(dto.getMachineId());
        entity.setEventTime(dto.getEventTime());
        entity.setReceivedTime(receivedTime);
        entity.setDurationMs(dto.getDurationMs());
        entity.setDefectCount(dto.getDefectCount());
        entity.setPayloadHash(payloadHash);
        entity.setFactoryId(dto.getFactoryId());
        entity.setLineId(dto.getLineId());

        return entity;
    }


    protected void validate(EventRequestDto dto) {
        if (dto.getDurationMs() < 0) {
            throw new IllegalArgumentException("INVALID_DURATION");
        }

        long sixHoursMs = 6L * 60 * 60 * 1000;
        if (dto.getDurationMs() > sixHoursMs) {
            throw new IllegalArgumentException("INVALID_DURATION");
        }

        Instant nowPlus15 = Instant.now().plusSeconds(15 * 60);
        if (dto.getEventTime().isAfter(nowPlus15)) {
            throw new IllegalArgumentException("EVENT_TIME_IN_FUTURE");
        }
    }

    private String computePayloadHash(EventRequestDto dto) {
        return dto.getEventId()
                + "|" + dto.getMachineId()
                + "|" + dto.getEventTime()
                + "|" + dto.getDurationMs()
                + "|" + dto.getDefectCount()
                + "|" + dto.getFactoryId()
                + "|" + dto.getLineId();
    }



    enum IngestResult {
        ACCEPTED,
        UPDATED,
        DEDUPED,
        IGNORED,
        REJECTED
    }
}

