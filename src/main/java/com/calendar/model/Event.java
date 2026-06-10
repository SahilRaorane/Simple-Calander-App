package com.calendar.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Event {

    private String id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // Required by Jackson
    public Event() {}

    public Event(String title, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean overlapsWith(Event other) {
        if (!this.date.equals(other.date)) return false;
        // Two intervals [a,b) and [c,d) overlap when a < d AND c < b
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    // Getters
    public String getId()            { return id; }
    public String getTitle()         { return title; }
    public LocalDate getDate()       { return date; }
    public LocalTime getStartTime()  { return startTime; }
    public LocalTime getEndTime()    { return endTime; }

    // Setters (required by Jackson)
    public void setId(String id)                 { this.id = id; }
    public void setTitle(String title)           { this.title = title; }
    public void setDate(LocalDate date)          { this.date = date; }
    public void setStartTime(LocalTime startTime){ this.startTime = startTime; }
    public void setEndTime(LocalTime endTime)    { this.endTime = endTime; }

    @Override
    public String toString() {
        return String.format("  [%s] %-30s  %s - %s", id, title, startTime, endTime);
    }
}
