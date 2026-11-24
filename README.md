# HAVEN / STAYS — Luxury Hotel Reservation Portal

A Java 17+ Swing desktop application for IntelliJ IDEA, using the existing SQLite reservation workflow and locally bundled room photography.

## Open in IntelliJ IDEA
1. Extract the ZIP file.
2. In IntelliJ IDEA choose **File → Open** and select this project folder (the folder containing `pom.xml`).
3. Choose **Trust Project** if prompted and allow Maven to import the project.
4. Ensure the Project SDK is set to **Java 17** or newer.
5. Open `src/main/java/HotelReservationSystem.java` and run `main()`; alternatively use the Maven `exec:java` goal.

SQLite JDBC is the only runtime dependency and is declared in `pom.xml`. It is required for SQLite connectivity; the UI itself uses only standard Java Swing/AWT. The database file `hotel_reservations.db` is created in the application's working directory on first run.

## Included
- Midnight Slate / Champagne Gold brand header, custom skyline mark, centered navigation, and active Home indicator.
- Full-bleed twilight beachfront hero photo (`src/main/resources/images/home_hero.jpg`) with cover scaling and a dark vignette overlay.
- Dashboard KPI cards, room portfolio cards and bundled room photos in `src/main/resources/images/`.
- Quick actions, reservation table with green status pills, and live date/time status bar.
- Glass-style availability card with date, room and guest selectors. Availability checks the existing SQLite reservations table and then opens the existing booking form. Existing booking, reservation, room and SQLite workflows are retained.
- Safe fallback text if a room image resource is unavailable.

## Notes
This is a desktop Java Swing application, not a hosted website. The current data model contains rooms, guests and reservations; the other sidebar destinations are navigation shortcuts to the closest implemented screens rather than separate payment/report/settings modules.

## Dashboard header upgrade
The dashboard now uses a `DashboardHeaderPanel` in the center workspace. It paints a left-to-right Alabaster fade over a bundled hero image and positions four reusable `RoundedPanel` KPI cards across the lower edge. Add a hotel lobby photograph at `src/main/resources/images/lobby-banner.jpg` (or `.png`) to use your own lobby image; when absent, the bundled suite-room image is used automatically. KPI values remain connected to the live SQLite counts rather than hard-coded demo totals.

## Rooms view upgrade
The Rooms & Facilities view now includes a full-bleed featured-room hero with a dark gradient, translucent booking widget, date/night controls, guest spinners, and a SQLite-backed room-availability search. The lower horizontal gallery includes four selectable room cards and working category filters / scroll arrows. Selecting a room updates the hero details, booking room type, and the existing reservation room selector. Existing SQLite schema, reservation operations, and navigation remain in place.

Room image resources are under `src/main/resources/images/`: `featured_room.jpg`, `standard.jpg`, `deluxe.jpg`, `suite.jpg`, and `penthouse.jpg`. Replace these files with your preferred high-resolution hotel photography while keeping the filenames unchanged.

## Reservations view upgrade
The Reservations workspace now includes a two-row customer-information grid with focused underline inputs, an immediate-check-in toggle, and a multi-room Room Information table. Room rows support room selection, nights, nightly price, extra-bed quantity/rate, person count, check-in/out dates, and automatically calculated row and grand totals. The + / − controls add and remove room rows.

Confirm validates the guest and dates, checks room conflicts against existing confirmed bookings, and saves all selected room reservations in one SQLite transaction. Existing databases are upgraded in place with additive guest and reservation columns for secondary phone, ID number, immediate check-in, nights, nightly rate, extra-bed values, and total amount; existing reservation queries remain compatible.

## Visual alignment correction
The application shell has been updated to better match the supplied HAVEN / STAYS reservation reference: a midnight-slate top brand/title bar, left-side workspace navigation, champagne-gold active Reservations item, and a wider reservation workspace. The reservation form and room table retain the SQLite-backed save workflow.

The Java source was compiled with Java 17-compatible `javac` after this update. Runtime database testing still requires Maven to resolve the SQLite JDBC dependency.
