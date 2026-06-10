package com.calendar;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.calendar.model.Event;
import com.calendar.service.CalendarService;
import com.calendar.storage.FileStorage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarServiceTest {

    private static final String TEST_FILE = "data/test-events.json";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private CalendarService service;

    @BeforeEach
    void setUp() {
        // Fresh storage for every test
        new File(TEST_FILE).delete();
        service = new CalendarService(new FileStorage(TEST_FILE));
    }

    @AfterEach
    void tearDown() {
        new File(TEST_FILE).delete();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // addEvent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void addEvent_successfullyCreatesEvent() {
        Event e = service.addEvent("Team Standup", TODAY, time(9, 0), time(9, 30));
        assertNotNull(e.getId());
        assertEquals("Team Standup", e.getTitle());
        assertEquals(TODAY, e.getDate());
        assertEquals(time(9, 0),  e.getStartTime());
        assertEquals(time(9, 30), e.getEndTime());
    }

    @Test
    @Order(2)
    void addEvent_rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("  ", TODAY, time(9, 0), time(9, 30)));
    }

    @Test
    @Order(2)
    void addEvent_rejectsNullTitle() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent(null, TODAY, time(9, 0), time(9, 30)));
    }

    @Test
    @Order(3)
    void addEvent_rejectsPastDate() {
        LocalDate yesterday = TODAY.minusDays(1);
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Past Event", yesterday, time(9, 0), time(10, 0)));
    }

    @Test
    @Order(4)
    void addEvent_rejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Bad Event", TODAY, time(10, 0), time(9, 0)));
    }

    @Test
    @Order(4)
    void addEvent_rejectsStartEqualToEnd() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Zero-length", TODAY, time(10, 0), time(10, 0)));
    }

    @Test
    @Order(5)
    void addEvent_rejectsDirectOverlap() {
        service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));

        // Exact same slot
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Duplicate", TODAY, time(10, 0), time(11, 0)));
    }

    @Test
    @Order(6)
    void addEvent_rejectsPartialOverlap_startInside() {
        service.addEvent("Morning Meeting", TODAY, time(9, 0), time(10, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Overlap", TODAY, time(9, 30), time(10, 30)));
    }

    @Test
    @Order(7)
    void addEvent_rejectsPartialOverlap_endsInside() {
        service.addEvent("Morning Meeting", TODAY, time(9, 0), time(10, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Overlap", TODAY, time(8, 30), time(9, 30)));
    }

    @Test
    @Order(8)
    void addEvent_rejectsContainedEvent() {
        service.addEvent("Long Block", TODAY, time(9, 0), time(12, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.addEvent("Inner", TODAY, time(10, 0), time(11, 0)));
    }

    @Test
    @Order(9)
    void addEvent_allowsAdjacentEvents() {
        // End of first == start of second — should NOT overlap
        service.addEvent("First",  TODAY, time(9, 0),  time(10, 0));
        assertDoesNotThrow(
            () -> service.addEvent("Second", TODAY, time(10, 0), time(11, 0)));
    }

    @Test
    @Order(10)
    void addEvent_allowsSameSlotsOnDifferentDays() {
        service.addEvent("Today's Meeting",    TODAY,    time(9, 0), time(10, 0));
        assertDoesNotThrow(
            () -> service.addEvent("Tomorrow's Meeting", TOMORROW, time(9, 0), time(10, 0)));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEventsForDay
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    void getEventsForDay_returnsEventsInChronologicalOrder() {
        service.addEvent("C", TODAY, time(14, 0), time(15, 0));
        service.addEvent("A", TODAY, time(9,  0), time(10, 0));
        service.addEvent("B", TODAY, time(11, 0), time(12, 0));

        List<Event> result = service.getEventsForDay(TODAY);
        assertEquals(3, result.size());
        assertEquals("A", result.get(0).getTitle());
        assertEquals("B", result.get(1).getTitle());
        assertEquals("C", result.get(2).getTitle());
    }

    @Test
    @Order(12)
    void getEventsForDay_returnsEmptyForDayWithNoEvents() {
        service.addEvent("Today Only", TODAY, time(9, 0), time(10, 0));
        List<Event> result = service.getEventsForDay(TOMORROW);
        assertTrue(result.isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getRemainingEventsForToday
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(13)
    void getRemainingToday_returnsEmptyWhenNoEventsToday() {
        service.addEvent("Tomorrow Event", TOMORROW, time(9, 0), time(10, 0));
        assertTrue(service.getRemainingEventsForToday().isEmpty());
    }

    @Test
    @Order(14)
    void getRemainingToday_excludesAlreadyEndedEvents() {
        // Ends at 00:02 — will have passed by the time any realistic test run executes
        service.addEvent("Midnight Past", TODAY, time(0, 0), time(0, 2));
        assertTrue(service.getRemainingEventsForToday().isEmpty());
    }

    @Test
    @Order(15)
    void getRemainingToday_includesFutureEvents() {
        // Ends near midnight — will not have passed during any realistic test run
        service.addEvent("Late Event", TODAY, time(23, 57), time(23, 59));
        List<Event> results = service.getRemainingEventsForToday();
        assertEquals(1, results.size());
        assertEquals("Late Event", results.get(0).getTitle());
    }

    @Test
    @Order(16)
    void getRemainingToday_excludesEventsOnOtherDays() {
        service.addEvent("Tomorrow", TOMORROW, time(23, 57), time(23, 59));
        assertTrue(service.getRemainingEventsForToday().isEmpty());
    }

    @Test
    @Order(17)
    void getRemainingToday_returnsSortedByStartTime() {
        service.addEvent("Late",  TODAY, time(23, 50), time(23, 55));
        service.addEvent("Early", TODAY, time(23, 30), time(23, 45));
        List<Event> results = service.getRemainingEventsForToday();
        assertEquals(2, results.size());
        assertEquals("Early", results.get(0).getTitle());
        assertEquals("Late",  results.get(1).getTitle());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getNextAvailableSlot
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(13)
    void getNextAvailableSlot_returnsSlotOnEmptyDay() {
        // On a future date (no current-time restriction), first slot = DAY_START
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 60);
        assertEquals(CalendarService.DAY_START, slot);
    }

    @Test
    @Order(14)
    void getNextAvailableSlot_returnsSlotAfterExistingEvent() {
        service.addEvent("Morning Block", TOMORROW, time(8, 0), time(12, 0));
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 30);
        assertEquals(time(12, 0), slot);
    }

    @Test
    @Order(15)
    void getNextAvailableSlot_returnsSlotInGapBetweenEvents() {
        service.addEvent("Morning",   TOMORROW, time(8,  0), time(10, 0));
        service.addEvent("Afternoon", TOMORROW, time(12, 0), time(14, 0));
        // 2-hour gap exists 10:00–12:00
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 90);
        assertEquals(time(10, 0), slot);
    }

    @Test
    @Order(16)
    void getNextAvailableSlot_returnsNullWhenFullyBooked() {
        // Fill entire business day
        service.addEvent("Block", TOMORROW, time(8, 0), time(20, 0));
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 30);
        assertNull(slot);
    }

    @Test
    @Order(17)
    void getNextAvailableSlot_returnsNullWhenSlotDoesNotFit() {
        // Only 30 minutes left but we need 60
        service.addEvent("Block", TOMORROW, time(8, 0), time(19, 30));
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 60);
        assertNull(slot);
    }

    @Test
    @Order(18)
    void getNextAvailableSlot_rejectsZeroDuration() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getNextAvailableSlot(TOMORROW, 0));
    }

    @Test
    @Order(18)
    void getNextAvailableSlot_rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getNextAvailableSlot(TOMORROW, -30));
    }

    @Test
    @Order(18)
    void getNextAvailableSlot_returnsSlotThatFitsExactlyAtDayEnd() {
        // Event 08:00–19:00 leaves exactly 60 min (19:00–20:00); should succeed
        service.addEvent("Long Block", TOMORROW, time(8, 0), time(19, 0));
        LocalTime slot = service.getNextAvailableSlot(TOMORROW, 60);
        assertEquals(time(19, 0), slot);
    }

    @Test
    @Order(19)
    void getNextAvailableSlot_rejectsPastDate() {
        LocalDate yesterday = TODAY.minusDays(1);
        assertThrows(IllegalArgumentException.class,
            () -> service.getNextAvailableSlot(yesterday, 30));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(19)
    void persistence_eventsAreReloadedFromFile() {
        service.addEvent("Persisted", TODAY, time(10, 0), time(11, 0));

        // New service instance reads from the same file
        CalendarService reloaded = new CalendarService(new FileStorage(TEST_FILE));
        List<Event> loaded = reloaded.getEventsForDay(TODAY);

        assertEquals(1, loaded.size());
        assertEquals("Persisted", loaded.get(0).getTitle());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // searchEventsByTitle
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void search_returnsMatchingEventsCaseInsensitive() {
        service.addEvent("Team Standup",  TODAY,    time(9,  0), time(9,  30));
        service.addEvent("Team Planning", TOMORROW, time(10, 0), time(11, 0));
        service.addEvent("Code Review",   TODAY,    time(14, 0), time(15, 0));

        List<Event> results = service.searchEventsByTitle("team");
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getTitle().toLowerCase().contains("team")));
    }

    @Test
    @Order(21)
    void search_partialMatchWorks() {
        service.addEvent("Sprint Planning", TODAY, time(10, 0), time(11, 0));
        service.addEvent("Release Review",  TODAY, time(14, 0), time(15, 0));

        List<Event> results = service.searchEventsByTitle("plan");
        assertEquals(1, results.size());
        assertEquals("Sprint Planning", results.get(0).getTitle());
    }

    @Test
    @Order(22)
    void search_blankKeywordReturnsAll() {
        service.addEvent("Meeting A", TODAY,    time(9,  0), time(10, 0));
        service.addEvent("Meeting B", TOMORROW, time(10, 0), time(11, 0));

        assertEquals(2, service.searchEventsByTitle("").size());
        assertEquals(2, service.searchEventsByTitle(null).size());
    }

    @Test
    @Order(23)
    void search_returnsEmptyWhenNoMatch() {
        service.addEvent("Stand-up", TODAY, time(9, 0), time(9, 30));
        assertTrue(service.searchEventsByTitle("xyz").isEmpty());
    }

    @Test
    @Order(24)
    void search_resultsSortedByDateThenTime() {
        service.addEvent("B Event", TOMORROW, time(10, 0), time(11, 0));
        service.addEvent("A Event", TOMORROW, time(9,  0), time(10, 0));
        service.addEvent("C Event", TODAY,    time(8,  0), time(9,  0));

        List<Event> results = service.searchEventsByTitle("");
        assertEquals("C Event", results.get(0).getTitle());
        assertEquals("A Event", results.get(1).getTitle());
        assertEquals("B Event", results.get(2).getTitle());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateEvent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void updateEvent_successfullyUpdatesAllFields() {
        Event e = service.addEvent("Old Title", TODAY, time(10, 0), time(11, 0));
        Event updated = service.updateEvent(e.getId(), "New Title", TOMORROW, time(9, 0), time(10, 0));

        assertEquals(e.getId(),     updated.getId());
        assertEquals("New Title",   updated.getTitle());
        assertEquals(TOMORROW,      updated.getDate());
        assertEquals(time(9, 0),    updated.getStartTime());
        assertEquals(time(10, 0),   updated.getEndTime());
    }

    @Test
    @Order(21)
    void updateEvent_keepsExistingValuesWhenNullPassed() {
        Event e = service.addEvent("Keep Me", TODAY, time(10, 0), time(11, 0));
        Event updated = service.updateEvent(e.getId(), null, null, null, null);

        assertEquals("Keep Me",    updated.getTitle());
        assertEquals(TODAY,         updated.getDate());
        assertEquals(time(10, 0),   updated.getStartTime());
        assertEquals(time(11, 0),   updated.getEndTime());
    }

    @Test
    @Order(22)
    void updateEvent_rejectsUnknownId() {
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent("XXXXXXXX", "Title", TODAY, time(9, 0), time(10, 0)));
    }

    @Test
    @Order(23)
    void updateEvent_rejectsPastDate() {
        Event e = service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        LocalDate yesterday = TODAY.minusDays(1);
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent(e.getId(), null, yesterday, null, null));
    }

    @Test
    @Order(24)
    void updateEvent_rejectsStartAfterEnd() {
        Event e = service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent(e.getId(), null, null, time(12, 0), time(11, 0)));
    }

    @Test
    @Order(24)
    void updateEvent_rejectsStartEqualToEnd() {
        Event e = service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent(e.getId(), null, null, time(10, 0), time(10, 0)));
    }

    @Test
    @Order(24)
    void updateEvent_rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent("  ", "Title", TODAY, time(9, 0), time(10, 0)));
    }

    @Test
    @Order(24)
    void updateEvent_rejectsNullId() {
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent(null, "Title", TODAY, time(9, 0), time(10, 0)));
    }

    @Test
    @Order(25)
    void updateEvent_rejectsOverlapWithOtherEvent() {
        service.addEvent("Blocker",  TODAY, time(10, 0), time(12, 0));
        Event target = service.addEvent("Target", TODAY, time(14, 0), time(15, 0));
        // Try to move Target into Blocker's slot
        assertThrows(IllegalArgumentException.class,
            () -> service.updateEvent(target.getId(), null, null, time(11, 0), time(13, 0)));
    }

    @Test
    @Order(26)
    void updateEvent_doesNotConflictWithItself() {
        // Updating only the title should never throw an overlap error against itself
        Event e = service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        assertDoesNotThrow(
            () -> service.updateEvent(e.getId(), "Renamed Meeting", null, null, null));
    }

    @Test
    @Order(27)
    void updateEvent_persistsChangesToFile() {
        Event e = service.addEvent("Original", TODAY, time(10, 0), time(11, 0));
        service.updateEvent(e.getId(), "Updated", null, null, null);

        CalendarService reloaded = new CalendarService(new FileStorage(TEST_FILE));
        assertEquals("Updated", reloaded.getAllEvents().get(0).getTitle());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteEvent
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    void deleteEvent_removesEventById() {
        Event e = service.addEvent("To Delete", TODAY, time(10, 0), time(11, 0));
        String id = e.getId();

        Event removed = service.deleteEvent(id);

        assertEquals(id, removed.getId());
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    @Order(21)
    void deleteEvent_isCaseInsensitive() {
        Event e = service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        // IDs are stored uppercase; pass lowercase to verify normalisation
        assertDoesNotThrow(() -> service.deleteEvent(e.getId().toLowerCase()));
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    @Order(22)
    void deleteEvent_rejectsUnknownId() {
        service.addEvent("Meeting", TODAY, time(10, 0), time(11, 0));
        assertThrows(IllegalArgumentException.class,
            () -> service.deleteEvent("XXXXXXXX"));
    }

    @Test
    @Order(23)
    void deleteEvent_rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> service.deleteEvent("  "));
    }

    @Test
    @Order(24)
    void deleteEvent_onlyRemovesMatchingEvent() {
        service.addEvent("Keep",   TODAY, time(9,  0), time(10, 0));
        Event target = service.addEvent("Remove", TODAY, time(11, 0), time(12, 0));

        service.deleteEvent(target.getId());

        List<Event> remaining = service.getAllEvents();
        assertEquals(1, remaining.size());
        assertEquals("Keep", remaining.get(0).getTitle());
    }

    @Test
    @Order(25)
    void deleteEvent_persistsRemovalToFile() {
        Event e = service.addEvent("Temp", TODAY, time(10, 0), time(11, 0));
        service.deleteEvent(e.getId());

        CalendarService reloaded = new CalendarService(new FileStorage(TEST_FILE));
        assertTrue(reloaded.getAllEvents().isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────

    private static LocalTime time(int hour, int minute) {
        return LocalTime.of(hour, minute);
    }
}
