package com.company.factory_events.dto;

import java.time.Instant;

public class StatsResponseDto {

    private String machineId;
    private Instant start;
    private Instant end;

    private long eventsCount;
    private long defectsCount;
    private double avgDefectRate;
    private String status;

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }

    public long getEventsCount() {
        return eventsCount;
    }

    public void setEventsCount(long eventsCount) {
        this.eventsCount = eventsCount;
    }

    public long getDefectsCount() {
        return defectsCount;
    }

    public void setDefectsCount(long defectsCount) {
        this.defectsCount = defectsCount;
    }

    public double getAvgDefectRate() {
        return avgDefectRate;
    }

    public void setAvgDefectRate(double avgDefectRate) {
        this.avgDefectRate = avgDefectRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

