package com.calendar.service;

import com.calendar.model.Event;
import com.calendar.storage.FileStorage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarService {

    // Slot search bounds
    public static final LocalTime DAY_START = LocalTime.of(8, 0);
    public static final LocalTime DAY_END   = LocalTime.of(20, 0);

    private final FileStorage storage;
    private List<Event> events;

    public CalendarService(FileStorage storage) {
        this.storage = storage;
        this.events = storage.loadEvents();
    }

    /**
     * Add a new event after validating that it does not overlap any existing event on the same day.
     *
     * @throws IllegalArgumentException if start >= end, or if the event overlaps an existing one
     */
    public Event addEvent(String title, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Event title cannot be empty.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot add events in the past. Please choose today or a future date.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        Event newEvent = new Event(title.trim(), date, startTime, endTime);

        for (Event existing : events) {
            if (newEvent.overlapsWith(existing)) {
                throw new IllegalArgumentException(
                    "Overlaps with existing event: \"" + existing.getTitle() +
                    "\" (" + existing.getStartTime() + " - " + existing.getEndTime() + ")"
                );
            }
        }

        events.add(newEvent);
        storage.saveEvents(events);
        return newEvent;
    }

    /** All events on the given date, sorted by start time. */
    public List<Event> getEventsForDay(LocalDate date) {
        return events.stream()
            .filter(e -> e.getDate().equals(date))
            .sorted(Comparator.comparing(Event::getStartTime))
            .collect(Collectors.toList());
    }

    /**
     * Events on today whose end time is still in the future.
     * An in-progress event (started but not yet ended) is included.
     */
    public List<Event> getRemainingEventsForToday() {
        LocalDate today    = LocalDate.now();
        LocalTime nowTime  = LocalTime.now();
        return events.stream()
            .filter(e -> e.getDate().equals(today) && e.getEndTime().isAfter(nowTime))
            .sorted(Comparator.comparing(Event::getStartTime))
            .collect(Collectors.toList());
    }

    /**
     * Find the earliest time on the given day that a contiguous free slot of
     * {@code durationMinutes} minutes fits within business hours (08:00–20:00).
     *
     * For today the search starts at the current time; for any other date it
     * starts at DAY_START.
     *
     * @return the start of the next free slot, or {@code null} if none exists today.
     */
    public LocalTime getNextAvailableSlot(LocalDate date, int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be a positive number of minutes.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot find available slot for a past date.");
        }

        List<Event> dayEvents = getEventsForDay(date);

        LocalTime candidate = date.equals(LocalDate.now())
            ? LocalTime.now()
            : DAY_START;

        // Clamp to business hours start
        if (candidate.isBefore(DAY_START)) candidate = DAY_START;

        for (Event event : dayEvents) {
            LocalTime slotEnd = candidate.plusMinutes(durationMinutes);

            // Does the candidate slot fit before this event starts?
            if (!slotEnd.isAfter(event.getStartTime())) {
                // Fits — check it is within day end
                return slotEnd.isAfter(DAY_END) ? null : candidate;
            }

            // Push candidate past this event if still blocking
            if (candidate.isBefore(event.getEndTime())) {
                candidate = event.getEndTime();
            }
        }

        // Check remaining time after all events
        LocalTime slotEnd = candidate.plusMinutes(durationMinutes);
        return slotEnd.isAfter(DAY_END) ? null : candidate;
    }

    /**
     * Update an existing event. Only fields with non-null values are changed;
     * pass null to keep a field as-is.
     *
     * Validates: event must exist, new date (if changed) must not be in the past,
     * start must be before end, and the updated slot must not overlap any OTHER event.
     *
     * @throws IllegalArgumentException on any validation failure
     */
    public Event updateEvent(String id, String newTitle, LocalDate newDate,
                             LocalTime newStart, LocalTime newEnd) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be empty.");
        }
        String trimmed = id.trim().toUpperCase();

        Event target = null;
        for (Event e : events) {
            if (e.getId().equals(trimmed)) {
                target = e;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("No event found with ID: " + trimmed);
        }

        // Resolve final values (null = keep existing)
        String    resolvedTitle = (newTitle != null && !newTitle.trim().isEmpty()) ? newTitle.trim() : target.getTitle();
        LocalDate resolvedDate  = (newDate  != null) ? newDate  : target.getDate();
        LocalTime resolvedStart = (newStart != null) ? newStart : target.getStartTime();
        LocalTime resolvedEnd   = (newEnd   != null) ? newEnd   : target.getEndTime();

        if (resolvedDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot set event to a past date.");
        }
        if (!resolvedStart.isBefore(resolvedEnd)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // Build a temporary event to reuse overlapsWith(); exclude the event being updated
        Event proposed = new Event(resolvedTitle, resolvedDate, resolvedStart, resolvedEnd);
        for (Event existing : events) {
            if (existing.getId().equals(trimmed)) continue;
            if (proposed.overlapsWith(existing)) {
                throw new IllegalArgumentException(
                    "Updated slot overlaps with: \"" + existing.getTitle() +
                    "\" (" + existing.getStartTime() + " - " + existing.getEndTime() + ")"
                );
            }
        }

        target.setTitle(resolvedTitle);
        target.setDate(resolvedDate);
        target.setStartTime(resolvedStart);
        target.setEndTime(resolvedEnd);
        storage.saveEvents(events);
        return target;
    }

    /**
     * Delete the event with the given ID.
     *
     * @throws IllegalArgumentException if no event with that ID exists
     */
    public Event deleteEvent(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be empty.");
        }
        String trimmed = id.trim().toUpperCase();
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(trimmed)) {
                Event removed = events.remove(i);
                storage.saveEvents(events);
                return removed;
            }
        }
        throw new IllegalArgumentException("No event found with ID: " + trimmed);
    }

    /**
     * Return all events whose title contains {@code keyword} (case-insensitive).
     * Passing a blank keyword returns every event.
     * Results are sorted by date then start time.
     */
    public List<Event> searchEventsByTitle(String keyword) {
        String lower = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim().toLowerCase();
        return events.stream()
            .filter(e -> lower.isEmpty() || e.getTitle().toLowerCase().contains(lower))
            .sorted(Comparator.comparing(Event::getDate).thenComparing(Event::getStartTime))
            .collect(Collectors.toList());
    }

    /** Expose the full list (used by tests). */
    public List<Event> getAllEvents() {
        return events;
    }
}
