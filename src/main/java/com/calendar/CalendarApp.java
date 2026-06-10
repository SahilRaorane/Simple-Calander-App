package com.calendar;

import com.calendar.model.Event;
import com.calendar.service.CalendarService;
import com.calendar.storage.FileStorage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point — CLI-driven calendar application.
 *
 * Data is persisted to data/events.json relative to the working directory.
 */
public class CalendarApp {

    private static final String DATA_FILE = "data/events.json";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String DIVIDER =
        "-----------------------------------------------------";

    public static void main(String[] args) {
        CalendarService service = new CalendarService(new FileStorage(DATA_FILE));
        Scanner scanner = new Scanner(System.in);

        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    handleAddEvent(scanner, service);
                    break;
                case "2":
                    handleListToday(service);
                    break;
                case "3":
                    handleListRemaining(service);
                    break;
                case "4":
                    handleListForDay(scanner, service);
                    break;
                case "5":
                    handleNextSlot(scanner, service);
                    break;
                case "6":
                    handleSearch(scanner, service);
                    break;
                case "7":
                    handleUpdateEvent(scanner, service);
                    break;
                case "8":
                    handleDeleteEvent(scanner, service);
                    break;
                case "9":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please enter 1-9.");
            }
        }
        scanner.close();
    }

    // ----------------------------------------------------------
    // Menu handlers
    // ----------------------------------------------------------

    private static void handleAddEvent(Scanner scanner, CalendarService service) {
        System.out.println("-- Add Event -----------------------------------------");
        System.out.println("  (Type 'exit' at any prompt to return to menu)");
        String title;
        while (true) {
            title = prompt(scanner, "Title");
            if (isExit(title)) { System.out.println(); return; }
            if (!title.isEmpty()) break;
            System.out.println("  Title cannot be empty. Please enter a title.");
        }

        LocalDate date = promptFutureDate(scanner, "Date (yyyy-MM-dd) [Enter = today]");
        if (date == null) { System.out.println(); return; }

        LocalTime start = promptTime(scanner, "Start time (HH:mm)");
        LocalTime end   = promptTime(scanner, "End   time (HH:mm)");

        try {
            Event created = service.addEvent(title, date, start, end);
            System.out.println("\nEvent added:");
            System.out.println(created);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void handleListToday(CalendarService service) {
        LocalDate today = LocalDate.now();
        System.out.println("-- All events for today (" + today + ") ------------------");
        List<Event> list = service.getEventsForDay(today);
        printEvents(list, "No events scheduled for today.");
    }

    private static void handleListRemaining(CalendarService service) {
        System.out.println("-- Remaining events for today ------------------------");
        List<Event> list = service.getRemainingEventsForToday();
        printEvents(list, "No remaining events for today.");
    }

    private static void handleListForDay(Scanner scanner, CalendarService service) {
        System.out.println("-- Events for a specific day -------------------------");
        System.out.println("  (Type 'exit' to return to menu)");
        LocalDate date = promptDate(scanner, "Date (yyyy-MM-dd)", false);
        if (date == null) { System.out.println(); return; }
        System.out.println();
        System.out.println("Events for " + date + ":");
        List<Event> list = service.getEventsForDay(date);
        printEvents(list, "No events scheduled for " + date + ".");
    }

    private static void handleSearch(Scanner scanner, CalendarService service) {
        System.out.println("-- Search Events -------------------------------------");
        System.out.println("  (Type 'exit' to return to menu)");
        String keyword = prompt(scanner, "Search by title (Enter = show all)");
        if (isExit(keyword)) { System.out.println(); return; }
        List<Event> results = service.searchEventsByTitle(keyword);
        System.out.println();
        if (keyword.isEmpty()) {
            System.out.println("All events (sorted by date and time):");
        } else {
            System.out.println("Results for \"" + keyword + "\":");
        }
        printEventsWithDate(results, "No events found.");
    }

    private static void handleUpdateEvent(Scanner scanner, CalendarService service) {
        System.out.println("-- Update Event --------------------------------------");
        Event current = searchAndSelect(scanner, service, true);
        if (current == null) return;

        System.out.println("  (Press Enter to keep the current value)");

        // Title
        System.out.print("  Title [" + current.getTitle() + "]: ");
        String titleInput = scanner.nextLine().trim();
        String newTitle = titleInput.isEmpty() ? null : titleInput;

        // Date
        LocalDate newDate = null;
        while (true) {
            System.out.print("  Date  [" + current.getDate() + "]: ");
            String dateInput = scanner.nextLine().trim();
            if (dateInput.isEmpty()) break;
            try {
                LocalDate parsed = LocalDate.parse(dateInput, DATE_FMT);
                if (parsed.isBefore(LocalDate.now())) {
                    System.out.println("  Past dates are not allowed. Please enter today or a future date.");
                    continue;
                }
                newDate = parsed;
                break;
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date. Use format yyyy-MM-dd (e.g. 2026-06-09).");
            }
        }

        // Start time
        LocalTime newStart = null;
        while (true) {
            System.out.print("  Start [" + current.getStartTime() + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) break;
            try { newStart = LocalTime.parse(input, TIME_FMT); break; }
            catch (DateTimeParseException e) {
                System.out.println("  Invalid time. Use format HH:mm (e.g. 09:30).");
            }
        }

        // End time
        LocalTime newEnd = null;
        while (true) {
            System.out.print("  End   [" + current.getEndTime() + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) break;
            try { newEnd = LocalTime.parse(input, TIME_FMT); break; }
            catch (DateTimeParseException e) {
                System.out.println("  Invalid time. Use format HH:mm (e.g. 10:00).");
            }
        }

        try {
            Event updated = service.updateEvent(current.getId(), newTitle, newDate, newStart, newEnd);
            System.out.println("\nEvent updated:");
            System.out.printf("  %-10s %-30s %-12s %-7s %-7s%n",
                updated.getId(), updated.getTitle(), updated.getDate(),
                updated.getStartTime(), updated.getEndTime());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void handleDeleteEvent(Scanner scanner, CalendarService service) {
        System.out.println("-- Delete Event --------------------------------------");
        Event target = searchAndSelect(scanner, service, false);
        if (target == null) return;

        try {
            Event deleted = service.deleteEvent(target.getId());
            System.out.println("Deleted: \"" + deleted.getTitle() + "\" on " + deleted.getDate() +
                " (" + deleted.getStartTime() + " - " + deleted.getEndTime() + ")");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Ask for a title search term, display matching events, then ask for an ID.
     * If futureOnly is true, only events from today onward are shown (for Update).
     * Returns the matched Event, or null if nothing was found / ID not recognised.
     */
    private static Event searchAndSelect(Scanner scanner, CalendarService service, boolean futureOnly) {
        System.out.println("  (Type 'exit' to return to menu)");
        String keyword = prompt(scanner, "Search by title (Enter = show all)");
        if (isExit(keyword)) { System.out.println(); return null; }
        List<Event> results = service.searchEventsByTitle(keyword);

        if (futureOnly) {
            LocalDate today = LocalDate.now();
            results = results.stream()
                .filter(e -> !e.getDate().isBefore(today))
                .collect(java.util.stream.Collectors.toList());
        }

        System.out.println();
        if (results.isEmpty()) {
            String msg = futureOnly ? "No upcoming events match that search." : "No events match that search.";
            System.out.println("  " + msg);
            System.out.println();
            return null;
        }

        printEventsWithDate(results, "");

        String id = prompt(scanner, "Enter Event ID (or 'exit' to go back)");
        if (isExit(id)) { System.out.println(); return null; }
        String idUpper = id.trim().toUpperCase();
        for (Event e : results) {
            if (e.getId().equals(idUpper)) return e;
        }
        System.out.println("Error: No event found with ID: " + idUpper);
        System.out.println();
        return null;
    }

    private static void handleNextSlot(Scanner scanner, CalendarService service) {
        System.out.println("-- Next available slot --------------------------------");
        System.out.println("  (Type 'exit' at any prompt to return to menu)");
        LocalDate date = promptFutureDate(scanner, "Date (yyyy-MM-dd) [Enter = today]");
        if (date == null) { System.out.println(); return; }
        int duration = promptInt(scanner, "Duration in minutes");
        if (duration <= 0) { System.out.println(); return; }

        LocalTime slot = service.getNextAvailableSlot(date, duration);
        System.out.println();
        if (slot != null) {
            LocalTime slotEnd = slot.plusMinutes(duration);
            System.out.printf("Next available %d-minute slot on %s:%n  %s - %s%n",
                duration, date, slot, slotEnd);
        } else {
            System.out.printf(
                "No available %d-minute slot found on %s within business hours (08:00–20:00).%n",
                duration, date);
        }
        System.out.println();
    }

    // ----------------------------------------------------------
    // Input helpers
    // ----------------------------------------------------------

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static boolean isExit(String input) {
        return "exit".equalsIgnoreCase(input);
    }

    // Returns null if user types "exit" (back signal)
    private static LocalDate promptDate(Scanner scanner, String label, boolean allowEmpty) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (isExit(input)) return null;
            if (allowEmpty && input.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(input, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date. Use format yyyy-MM-dd (e.g. 2026-06-09).");
            }
        }
    }

    // Returns null if user types 0 (back signal)
    private static LocalDate promptFutureDate(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (isExit(input)) return null;
            LocalDate date = input.isEmpty() ? LocalDate.now() : null;
            if (date == null) {
                try {
                    date = LocalDate.parse(input, DATE_FMT);
                } catch (DateTimeParseException e) {
                    System.out.println("  Invalid date. Use format yyyy-MM-dd (e.g. 2026-06-09).");
                    continue;
                }
            }
            if (date.isBefore(LocalDate.now())) {
                System.out.println("  Past dates are not allowed. Please enter today or a future date.");
                continue;
            }
            return date;
        }
    }

    private static LocalTime promptTime(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                return LocalTime.parse(input, TIME_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid time. Use format HH:mm (e.g. 09:30).");
            }
        }
    }

    // Returns 0 if user types "exit" (back signal — callers check for <= 0)
    private static int promptInt(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (isExit(input)) return 0;
            try {
                int val = Integer.parseInt(input);
                if (val > 0) return val;
                System.out.println("  Please enter a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    // ----------------------------------------------------------
    // Display helpers
    // ----------------------------------------------------------

    private static void printEvents(List<Event> events, String emptyMessage) {
        if (events.isEmpty()) {
            System.out.println("  " + emptyMessage);
        } else {
            System.out.printf("  %-10s %-30s %-7s %-7s%n", "ID", "Title", "Start", "End");
            System.out.println("  " + DIVIDER);
            for (Event e : events) {
                System.out.printf("  %-10s %-30s %-7s %-7s%n",
                    e.getId(), e.getTitle(), e.getStartTime(), e.getEndTime());
            }
        }
        System.out.println();
    }

    private static void printEventsWithDate(List<Event> events, String emptyMessage) {
        if (events.isEmpty()) {
            System.out.println("  " + emptyMessage);
        } else {
            System.out.printf("  %-10s %-30s %-12s %-7s %-7s%n", "ID", "Title", "Date", "Start", "End");
            System.out.println("  " + DIVIDER);
            for (Event e : events) {
                System.out.printf("  %-10s %-30s %-12s %-7s %-7s%n",
                    e.getId(), e.getTitle(), e.getDate(), e.getStartTime(), e.getEndTime());
            }
        }
        System.out.println();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("+=====================================================+");
        System.out.println("|            Simple Calendar Application              |");
        System.out.println("+=====================================================+");
        System.out.println();
    }

    private static void printMenu() {
        String today = LocalDate.now().format(DATE_FMT);
        String time  = LocalTime.now().format(TIME_FMT);
        System.out.println(DIVIDER);
        System.out.println("  Date: " + today + "   Time: " + time);
        System.out.println("  Please select one of the following function:");
        System.out.println(DIVIDER);
        System.out.println("  1. Add Event");
        System.out.println("  2. List all events for today");
        System.out.println("  3. List remaining events for today");
        System.out.println("  4. List all events for a specific day");
        System.out.println("  5. Find next available slot");
        System.out.println("  6. Search Events");
        System.out.println("  7. Update Event");
        System.out.println("  8. Delete Event");
        System.out.println("  9. Exit");
        System.out.println(DIVIDER);
        System.out.print("  Select option: ");
    }
}
