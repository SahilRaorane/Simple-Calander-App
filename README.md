# Simple Calendar Application

A command-line calendar and appointment management system built in Java.

---

## Prerequisites

Before running the application, make sure you have the following installed:

| Requirement | Version | Download |
|-------------|---------|----------|
| JDK | 17 (recommended) or 8+ | https://adoptium.net |
| Maven | 3.9+ (bundled in `.mvn/`) | https://maven.apache.org |

> **Important:** You need a **JDK** (Java Development Kit), not just a **JRE** (Java Runtime Environment). The JDK includes the compiler (`javac`) which Maven needs to build the project. To verify you have the right one, run `javac -version` — if it prints a version number, you're good.

---

## Project Structure

```
Project/
├── src/
│   ├── main/java/com/calendar/
│   │   ├── CalendarApp.java              ← Entry point — CLI menu
│   │   ├── model/Event.java              ← Data model
│   │   ├── service/CalendarService.java  ← Business logic
│   │   └── storage/FileStorage.java      ← JSON persistence
│   └── test/java/com/calendar/
│       └── CalendarServiceTest.java      ← 41 unit tests
├── data/
│   └── events.json                       ← Seed data (auto-created on first run)
├── .mvn/                                 ← Bundled Maven (no install needed)
├── pom.xml                               ← Maven configuration
├── run.bat                               ← Windows script to build and run
└── run-tests.bat                         ← Windows script to run tests
```

---

## How to Run the Application

### Option 1 — Double Click (Windows, Easiest)
1. Navigate to your Project folder
2. Double click `run.bat`
3. The application builds and starts automatically

### Option 2 — Command Prompt / PowerShell
Open Command Prompt or PowerShell inside the Project folder and run:
```cmd
.mvn\apache-maven-3.9.6\bin\mvn.cmd package -q
java -jar target\calendar-app.jar
```

Or if Maven is installed on your system:
```cmd
mvn package -q
java -jar target\calendar-app.jar
```

### Option 3 — Run the JAR Directly (after building once)
```cmd
java -jar "C:\path\to\Project\target\calendar-app.jar"
```

### Option 4 — VS Code
1. Open the Project folder in VS Code
2. Install the **Extension Pack for Java** (by Microsoft)
3. Open `src/main/java/com/calendar/CalendarApp.java`
4. Click the **▶ Run** button above `public static void main`
5. The terminal opens at the bottom with the menu

### Option 5 — IntelliJ IDEA
1. Open IntelliJ IDEA → click **Open** → select the Project folder
2. IntelliJ detects Maven and asks to **"Load Maven Project"** — click it
3. Go to **File** → **Project Structure** → set SDK to JDK 17
4. Open `CalendarApp.java`
5. Right click → **"Run 'CalendarApp.main()'"**

---

## How to Run the Tests

### Option 1 — Double Click (Windows, Easiest)
Double click `run-tests.bat`

Expected output: **41 tests, 0 failures**

### Option 2 — Command Prompt / PowerShell
```cmd
.mvn\apache-maven-3.9.6\bin\mvn.cmd test
```

Or if Maven is installed on your system:
```cmd
mvn test
```

### Option 3 — VS Code
1. Open `src/test/java/com/calendar/CalendarServiceTest.java`
2. Click the **▶ Run** button above the class name to run all 41 tests
3. Results appear in the terminal with ✅ or ❌

### Option 4 — IntelliJ IDEA
1. Open `CalendarServiceTest.java`
2. Right click anywhere in the file → **"Run 'CalendarServiceTest'"**
3. Green bar at the bottom = all tests passing

### Option 5 — Maven Panel in IntelliJ
1. Click the **Maven** tab on the right side
2. Expand your project → **Lifecycle**
3. Double click **test**

---

## Menu Options

Once the application starts, you will see:

```
+=====================================================+
|            Simple Calendar Application              |
+=====================================================+

-----------------------------------------------------
  Date: 2026-06-09   Time: 10:30
  Please select one of the following function:
-----------------------------------------------------
  1. Add Event
  2. List all events for today
  3. List remaining events for today
  4. List all events for a specific day
  5. Find next available slot
  6. Update Event
  7. Delete Event
  8. Search Events by Title
  9. Exit
-----------------------------------------------------
  Select option:
```

---

## Approach and Design

The application is built around three distinct layers, each with a single responsibility:

- **Model Layer (`Event.java`)** — Defines what an event is. Owns the overlap detection logic. Auto-generates unique 8-character IDs using UUID.
- **Service Layer (`CalendarService.java`)** — All business rules and algorithms. Validates input, prevents overlaps, finds available slots, handles search.
- **Storage Layer (`FileStorage.java`)** — Reads and writes events to `data/events.json` using the Jackson library. Completely swappable.

### Key Design Decisions

- **Overlap detection** uses the standard interval formula: two events `[a,b)` and `[c,d)` overlap when `a < d AND c < b`. Back-to-back events are correctly allowed.
- **Next available slot** uses a linear O(n) scan — walks sorted events and pushes a candidate time forward until a slot fits within business hours (08:00–20:00).
- **Update** uses null-coalescing — pass `null` to keep existing value. The event skips its own overlap check so renaming never causes a false conflict.
- **Search** is case-insensitive — lowercases both keyword and title before comparing.
- **Persistence** is automatic — every add, update, and delete immediately saves to `data/events.json`.

---

## Seed Data

The application ships with pre-loaded events across three days:

| ID | Title | Date | Start | End |
|----|-------|------|-------|-----|
| SEED0001 | Team Standup | 2026-06-08 | 09:00 | 09:30 |
| SEED0002 | Sprint Planning | 2026-06-08 | 10:00 | 11:30 |
| SEED0003 | Lunch Break | 2026-06-08 | 13:00 | 14:00 |
| SEED0004 | Code Review Session | 2026-06-08 | 15:00 | 16:00 |
| SEED0007 | Team Sync | 2026-06-09 | 09:00 | 09:45 |
| SEED0005 | Architecture Discussion | 2026-06-09 | 10:00 | 11:00 |
| SEED0006 | Client Presentation | 2026-06-09 | 14:00 | 15:30 |
| SEED0008 | Release Planning | 2026-06-10 | 11:00 | 12:00 |
| SEED0009 | QA Walkthrough | 2026-06-10 | 14:00 | 15:00 |
| SEED0010 | Team Retrospective | 2026-06-10 | 16:00 | 17:00 |

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jackson Databind | 2.15.2 | JSON serialization/deserialization |
| Jackson Datatype JSR310 | 2.15.2 | Java 8 date/time support for Jackson |
| JUnit Jupiter | 5.10.0 | Unit testing framework |

---

## Troubleshooting

**"No compiler is provided. Perhaps you are running on a JRE rather than a JDK?"**
You have JRE installed. Download and install JDK 17 from https://adoptium.net

**"mvn is not recognized"**
Maven is not in your system PATH. Use the bundled Maven:
`.mvn\apache-maven-3.9.6\bin\mvn.cmd package -q`

**"Failed to delete calendar-app.jar"**
The app is still running in the background. Open Task Manager (Ctrl + Shift + Esc) → click the Details tab → find java.exe → right click → End Task. Repeat for all java.exe processes. Then run run.bat again.