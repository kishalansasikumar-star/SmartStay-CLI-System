package smartstay.ui;

import smartstay.core.*;
import smartstay.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

public class E245549_Main {
    private final Scanner input = new Scanner(System.in);
    private final Path file;
    private final E245549_Hotel hotel;
    private static final String[] TYPES = {"Standard", "Deluxe", "Suite", "Family Suite"};

    private E245549_Main(Path file) throws IOException {
        this.file = file;
        hotel = E245549_Storage.load(file, Clock.system(E245549_Hotel.ZONE));
    }

    public static void main(String[] args) {
        try {
            new E245549_Main(Path.of("data", "hotel.dat")).run();
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed.");
        } catch (IOException e) {
            System.err.println("Data file error. Last action may not have been saved: " + e.getMessage());
            System.exit(1);
        }
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        String value = input.nextLine().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("A value is required.");
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

    private String choose(String label, String... choices) {
        System.out.println(label);
        for (int i = 0; i < choices.length; i++) System.out.printf("  %d. %s%n", i + 1, choices[i]);
        int choice = number("Select");
        if (choice < 1 || choice > choices.length) throw new IllegalArgumentException("Choose a listed option.");
        return choices[choice - 1];
    }

    private boolean confirm(String message) {
        return ask(message + " (y/n)").equalsIgnoreCase("y");
    }

    private void saved(String message) throws IOException {
        E245549_Storage.save(file, hotel);
        System.out.println(message);
    }

    private void run() throws IOException {
        System.out.println("SmartStay Hotel Booking System");
        String menu = "Main";
        while (true) {
            try {
                System.out.println("\n" + menu + " menu");
                switch (menu) {
                    case "Main":
                        System.out.println("1 Administrator   2 Guest   0 Exit");
                        int role = number("Choice");
                        if (role == 0) return;
                        if (role == 1) menu = "Administrator";
                        else if (role == 2) menu = "Guest";
                        else System.out.println("Choose 0, 1 or 2.");
                        break;
                    case "Administrator":
                        System.out.println("1 Add room   2 Update room   3 Remove room   4 Change status   5 List rooms   0 Back");
                        int adminChoice = number("Choice");
                        if (adminChoice == 0) menu = "Main";
                        else admin(adminChoice);
                        break;
                    case "Guest":
                        System.out.println("1 Register   2 Find rooms   3 Book   4 Find by booking ID   5 Find by name");
                        System.out.println("6 View by check-in   7 Modify   8 Cancel   9 Invoice   0 Back");
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

    private E245549_Room.Status pickStatus() {
        String status = choose("Room status", "Available", "Reserved", "Under Maintenance");
        return E245549_Room.Status.valueOf(status.replace(" ", ""));
    }

    private void admin(int choice) throws IOException {
        switch (choice) {
            case 1:
            case 2:
                String id = ask("Room ID");
                if (choice == 2) hotel.room(id);
                String type = choose("Room type", TYPES);
                E245549_Room.Bed bed = E245549_Room.Bed.valueOf(choose("Bed", "Single", "Double", "Queen", "King"));
                E245549_Room room = E245549_Room.create(type, id, bed, number("Guests included in rate"), pickStatus());
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
                if (hotel.allRooms().isEmpty()) System.out.println("No rooms yet. Use Add room to start.");
                for (E245549_Room r : hotel.allRooms()) showRoom(r, hotel.statusToday(r));
                break;
            default: System.out.println("Choose a listed option.");
        }
    }

    private void showRoom(E245549_Room r, E245549_Room.Status status) {
        System.out.printf("%s | %s | %s bed | LKR %,d/night | %d included | Breakfast: %s | %s%n",
                r.id, r.type(), r.bed, r.dailyRate(), r.capacity, r.includesBreakfast() ? "yes" : "no",
                status == E245549_Room.Status.UnderMaintenance ? "Under Maintenance" : status);
    }

    private List<E245549_Room> available(String type, LocalDate start, LocalDate end) {
        List<E245549_Room> rooms = hotel.searchRooms(type, start, end);
        for (E245549_Room r : rooms) showRoom(r, E245549_Room.Status.Available);
        if (rooms.isEmpty()) System.out.println("No matching rooms for this stay.");
        return rooms;
    }

    private void findOrBook(boolean book) throws IOException {
        String document = "";
        if (book) {
            document = ask("Registered NIC/passport");
            hotel.guest(document);
        }
        String type = choose("Room type", TYPES);
        LocalDate start = date("Check-in");
        LocalDate end = date("Check-out");
        List<E245549_Room> rooms = available(type, start, end);
        if (!book || rooms.isEmpty()) return;
        E245549_Room selected = hotel.room(ask("Room ID from results"));
        if (!rooms.contains(selected)) throw new IllegalArgumentException("Choose a room from the results.");
        int count = number("Number of guests");
        if (confirm("Confirm booking with LKR 10,000 refundable advance?")) {
            E245549_Reservation b = hotel.reserve(document, selected.id, start, end, count);
            saved("Booked " + b.id + ". Advance recorded: LKR 10,000.");
        }
    }

    private void guest(int choice) throws IOException {
        switch (choice) {
            case 1:
                hotel.register(new E245549_Guest(ask("NIC/passport"), ask("Full name"),
                        ask("Contact number"), ask("Email")));
                saved("Guest registered.");
                break;
            case 2: findOrBook(false); break;
            case 3: findOrBook(true); break;
            case 4: show(hotel.booking(ask("Booking ID"))); break;
            case 5: showList(hotel.byName(ask("Full guest name"))); break;
            case 6:
                boolean all = choose("Check-in view", "One date", "All dates in order").startsWith("All");
                showList(hotel.byDate(all ? null : date("Check-in")));
                break;
            case 7:
                E245549_Reservation old = hotel.booking(ask("Booking ID"));
                show(old);
                String roomId = ask("Room ID for updated stay");
                hotel.modify(old.id, roomId, date("Check-in"), date("Check-out"), number("Number of guests"));
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

    private void show(E245549_Reservation b) {
        System.out.printf("%s | %s | Room %s | %s to %s | Guests: %d | %s%n",
                b.id, b.guest.name, b.room.id, b.arrival, b.departure, b.guests,
                b.cancelled() ? "Cancelled / advance refunded" : "Confirmed");
    }

    private void showList(List<E245549_Reservation> list) {
        if (list.isEmpty()) System.out.println("No matching reservations.");
        for (E245549_Reservation b : list) show(b);
    }

    private void invoice(E245549_Reservation b) {
        E245549_Invoice bill = new E245549_Invoice(b, new E245549_LongStayDiscount());
        System.out.println("\nSmartStay invoice " + b.id + " (LKR)");
        System.out.println(b.guest.name + " | " + b.guest.document + " | " + b.guest.phone + " | " + b.guest.email);
        System.out.printf("Room %s | %s | %s bed | Daily rate %,d%n", b.room.id, b.room.type(), b.room.bed, b.room.dailyRate());
        System.out.printf("Check-in %s | Check-out %s | Nights %d | Guests %d%n", b.arrival, b.departure, bill.nights, b.guests);
        System.out.printf("Base charge: %,.2f%nExtra guest charges: %,.2f%nDiscount: %,.2f%n", bill.base, bill.extra, bill.discount);
        System.out.printf("Tax (%d%%): %,.2f%nTotal: %,.2f%nAdvance deducted: %,d.00%n",
                b.room.taxPercent(), bill.tax, bill.total, E245549_Reservation.ADVANCE);
        System.out.printf("Final payable: %,.2f%n", bill.balance.max(java.math.BigDecimal.ZERO));
        if (bill.balance.signum() < 0) System.out.printf("Refund due: %,.2f%n", bill.balance.negate());
    }
}
