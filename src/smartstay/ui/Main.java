package smartstay.ui;

import smartstay.core.*;
import smartstay.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    private final Scanner input = new Scanner(System.in);
    private final Path file;
    private final Hotel hotel;

    private Main(Path file) throws IOException {
        this.file = file;
        hotel = Storage.load(file, Clock.system(Hotel.ZONE));
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            new Main(Path.of("data", "hotel.dat")).run();
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed.");
        } catch (IOException e) {
            System.err.println("Data file error. Last action may not have been saved: " + e.getMessage());
            System.exit(1);
        }
    }

    private String ask(String label) {
        System.out.print(label + (label.endsWith("?") ? " " : ": "));
        String value = input.nextLine().trim();
        if (value.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return value;
    }

    private int number(String label) {
        try { return Integer.parseInt(ask(label)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number."); }
    }

    private LocalDate date(String label) {
        try { return LocalDate.parse(ask(label + " (YYYY-MM-DD)")); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Enter a valid date as YYYY-MM-DD."); }
    }

    private LocalTime checkInTime() {
        String value = ask("Check-in time (HH:mm, 24-hour Sri Lanka time)");
        try {
            if (!value.matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeParseException("Format", value, 0);
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Enter a valid check-in time, for example 14:00 or 15:30.");
        }
    }

    private void box(String title, String... rows) {
        int width = title.length();
        for (String row : rows) width = Math.max(width, row.length());
        String border = "─".repeat(width + 2);
        System.out.println("┌" + border + "┐");
        System.out.printf("│ %-" + width + "s │%n", title);
        if (rows.length > 0) System.out.println("├" + border + "┤");
        for (String row : rows) System.out.printf("│ %-" + width + "s │%n", row);
        System.out.println("└" + border + "┘");
    }

    private String choose(String label, String... choices) {
        String[] rows = new String[choices.length];
        for (int i = 0; i < choices.length; i++) rows[i] = String.format(Locale.ROOT, "[%02d] %s", i + 1, choices[i]);
        box(label, rows);
        int choice = number("Select");
        if (choice < 1 || choice > choices.length) throw new IllegalArgumentException("Choose a listed option.");
        return choices[choice - 1];
    }

    private String chooseRoomPackage() {
        RoomPackage[] packages = RoomPackage.values();
        String[] rows = new String[packages.length + 1];
        rows[0] = "     Room type      LKR/night   Free breakfast   LKR/extra guest   Tax";
        for (int i = 0; i < packages.length; i++) {
            RoomPackage roomPackage = packages[i];
            rows[i + 1] = String.format(Locale.US, "[%02d] %-13s %,9d   %-14s %,15d   %2d%%",
                    i + 1, roomPackage.label, roomPackage.dailyRate, roomPackage.breakfast ? "Yes" : "No", roomPackage.extraGuestRate, roomPackage.taxPercent);
        }
        box("Available Room Packages", rows);
        System.out.println("Extra guest rates are per person, per night, above the room's included guest count.");
        int choice = number("Select room package");
        if (choice < 1 || choice > packages.length) throw new IllegalArgumentException("Choose a listed room package.");
        return packages[choice - 1].label;
    }

    private boolean confirm(String message) {
        return ask(message + " (y/n)").equalsIgnoreCase("y");
    }

    private void saved(String message) throws IOException {
        Storage.save(file, hotel);
        System.out.println(message);
    }

    private void run() throws IOException {
        String menu = "Main";
        while (true) {
            try {
                System.out.println();
                switch (menu) {
                    case "Main":
                        box("SmartStay Hotel Booking System", "[01] Administrator", "[02] Guest", "[00] Exit");
                        int role = number("Choice");
                        if (role == 0) {
                            System.out.println("SmartStay closed successfully.");
                            return;
                        }
                        if (role == 1) menu = "Administrator";
                        else if (role == 2) menu = "Guest";
                        else System.out.println("Choose [01], [02] or [00].");
                        break;
                    case "Administrator":
                        box("Administrator menu", "[01] Add Room Record", "[02] Update Room Record", "[03] Remove Room Record",
                                "[04] Manage Room Availability", "[05] View All Room Records", "[00] Back");
                        int adminChoice = number("Choice");
                        if (adminChoice == 0) menu = "Main";
                        else admin(adminChoice);
                        break;
                    case "Guest":
                        box("Guest menu", "[01] Register as Customer", "[02] Search Available Rooms", "[03] Make Reservation",
                                "[04] Search by Booking ID", "[05] Search by Guest Name", "[06] View Reservations by Check-in Date",
                                "[07] Modify Reservation", "[08] Cancel Reservation", "[09] View Invoice", "[00] Back");
                        int guestChoice = number("Choice");
                        if (guestChoice == 0) menu = "Main";
                        else guest(guestChoice);
                        break;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Please check your entry: " + e.getMessage());
            }
        }
    }

    private Room.Status pickStatus() {
        String status = choose("Room status", "Available", "Reserved", "Under Maintenance");
        return Room.Status.valueOf(status.replace(" ", ""));
    }

    private void admin(int choice) throws IOException {
        switch (choice) {
            case 1:
            case 2:
                String id = ask("Room ID");
                if (choice == 2) hotel.room(id);
                String type = chooseRoomPackage();
                Room.Bed bed = Room.Bed.valueOf(choose("Bed Type", "Single", "Double", "Queen", "King"));
                Room room = Room.create(type, id, bed, number("How many guests does the nightly rate cover?"), pickStatus());
                if (choice == 1) hotel.addRoom(room);
                else hotel.updateRoom(room);
                saved("Room saved.");
                break;
            case 3:
                String removeId = ask("Room ID");
                if (confirm("Remove room " + removeId + "?")) {
                    hotel.removeRoom(removeId);
                    saved("Room removed.");
                }
                break;
            case 4:
                hotel.changeStatus(ask("Room ID"), pickStatus());
                saved("Status updated.");
                break;
            case 5:
                if (hotel.allRooms().isEmpty()) System.out.println("No rooms yet. Use Add Room Record to start.");
                for (Room listedRoom : hotel.allRooms()) showRoom(listedRoom, hotel.statusToday(listedRoom));
                break;
            default: System.out.println("Choose a listed option.");
        }
    }

    private void showRoom(Room room, Room.Status status) {
        box("Room " + room.id + "   " + room.type(),
                room.bed + " bed   |   " + String.format("LKR %,d/night", room.dailyRate()),
                "Guests included: " + room.capacity + "   |   Breakfast: " + (room.includesBreakfast() ? "yes" : "no"),
                "Status: " + (status == Room.Status.UnderMaintenance ? "Under Maintenance" : status));
    }

    private List<Room> available(String type, LocalDate start, LocalDate end) {
        List<Room> rooms = hotel.searchRooms(type, start, end);
        for (Room room : rooms) showRoom(room, Room.Status.Available);
        if (rooms.isEmpty()) System.out.println("No matching rooms for this stay.");
        return rooms;
    }

    private void findOrBook(boolean book) throws IOException {
        String document = "";
        if (book) {
            document = ask("Registered NIC/passport");
            hotel.guest(document);
        }
        String type = chooseRoomPackage();
        LocalDate start = date("Check-in");
        LocalDate end = date("Check-out");
        List<Room> rooms = available(type, start, end);
        if (rooms.isEmpty()) return;
        if (!book) {
            box("Room search", "[01] Make Reservation from These Results", "[00] Back to Guest menu");
            int choice = number("Choice");
            if (choice == 0) return;
            if (choice != 1) throw new IllegalArgumentException("Choose [01] to book or [00] to go back.");
        }
        hotel.checkBookingNotice(start);
        if (!book) {
            document = ask("Registered NIC/passport");
            hotel.guest(document);
        }
        Room selectedRoom = hotel.room(ask("Room ID from results"));
        if (!rooms.contains(selectedRoom)) throw new IllegalArgumentException("Choose a room from the results.");
        LocalTime arrivalTime = checkInTime();
        int count = number("Number of guests");
        if (confirm("Confirm booking with LKR 10,000 refundable advance?")) {
            Reservation booking = hotel.reserve(document, selectedRoom.id, start, end, count, arrivalTime);
            saved("Booked " + booking.id + ". Advance recorded: LKR 10,000.");
        }
    }

    private void guest(int choice) throws IOException {
        switch (choice) {
            case 1:
                hotel.register(new Guest(ask("NIC/passport"), ask("Full name"),
                        ask("Contact number"), ask("Email")));
                saved("Guest registered.");
                break;
            case 2: findOrBook(false); break;
            case 3: findOrBook(true); break;
            case 4: show(hotel.booking(ask("Booking ID"))); break;
            case 5: showList(hotel.byName(ask("Full guest name"))); break;
            case 6:
                boolean showAllDates = choose("Check-in view", "One date", "All dates in order").startsWith("All");
                showList(hotel.byDate(showAllDates ? null : date("Check-in")));
                break;
            case 7:
                Reservation existingBooking = hotel.booking(ask("Booking ID"));
                show(existingBooking);
                String roomId = ask("Room ID for updated stay");
                LocalDate arrival = date("Check-in");
                LocalDate departure = date("Check-out");
                LocalTime arrivalTime = checkInTime();
                hotel.modify(existingBooking.id, roomId, arrival, departure, number("Number of guests"), arrivalTime);
                saved("Reservation updated. Original advance retained.");
                break;
            case 8:
                String id = ask("Booking ID");
                show(hotel.booking(id));
                if (confirm("Cancel this reservation?")) {
                    hotel.cancel(id);
                    saved("Cancelled. Advance refund recorded: LKR 10,000.");
                }
                break;
            case 9: invoice(hotel.booking(ask("Booking ID"))); break;
            default: System.out.println("Choose a listed option.");
        }
    }

    private void show(Reservation booking) {
        System.out.printf("%s | %s | Room %s | %s to %s | Guests: %d | %s%n",
                booking.id, booking.guest.name, booking.room.id, booking.arrival + " " + booking.checkInTime, booking.departure, booking.guests,
                booking.cancelled() ? "Cancelled / advance refunded" : "Confirmed");
    }

    private void showList(List<Reservation> list) {
        if (list.isEmpty()) System.out.println("No matching reservations.");
        for (Reservation booking : list) show(booking);
    }

    private void invoice(Reservation booking) {
        Invoice bill = new Invoice(booking, new LongStayDiscount());
        box("SmartStay invoice " + booking.id + " (LKR)");
        System.out.println(booking.guest.name + " | " + booking.guest.document + " | " + booking.guest.phone + " | " + booking.guest.email);
        System.out.printf("Room %s | %s | %s bed | Daily rate %,d%n", booking.room.id, booking.room.type(), booking.room.bed, booking.room.dailyRate());
        System.out.printf("Check-in %s | Check-out %s | Nights %d | Guests %d%n", booking.arrival + " " + booking.checkInTime, booking.departure, bill.nights, booking.guests);
        System.out.printf("Base charge: %,.2f%nExtra guest charges: %,.2f%nDiscount: %,.2f%n", bill.base, bill.extra, bill.discount);
        System.out.printf("Tax (%d%%): %,.2f%nTotal: %,.2f%nAdvance deducted: %,d.00%n",
                booking.room.taxPercent(), bill.tax, bill.total, Reservation.ADVANCE);
        System.out.printf("Final payable: %,.2f%n", bill.balance.max(java.math.BigDecimal.ZERO));
        if (bill.balance.signum() < 0) System.out.printf("Refund due: %,.2f%n", bill.balance.negate());
    }
}
