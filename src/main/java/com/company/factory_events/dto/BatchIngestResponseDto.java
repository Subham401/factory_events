package com.company.factory_events.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchIngestResponseDto {

    private int accepted;
    private int updated;
    private int deduped;
    private int rejected;
    private int ignored;

    private List<Rejection> rejections = new ArrayList<>();

    public static class Rejection {
        private String eventId;
        private String reason;

        public Rejection(String eventId, String reason) {
            this.eventId = eventId;
            this.reason = reason;
        }

        public String getEventId() {
            return eventId;
        }

        public String getReason() {
            return reason;
        }
    }

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getDeduped() {
        return deduped;
    }

    public void setDeduped(int deduped) {
        this.deduped = deduped;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public List<Rejection> getRejections() {
        return rejections;
    }

    public void setRejections(List<Rejection> rejections) {
        this.rejections = rejections;
    }

    public int getIgnored() {
        return ignored;
    }

    public void setIgnored(int ignored) {
        this.ignored = ignored;
    }
}

