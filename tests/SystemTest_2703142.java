import smartstay.core.*;
import smartstay.model.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;

public class SystemTest_2703142 {
    private static int passed;
    private static void check(String name, boolean result) {
        if (!result) throw new AssertionError(name);
        System.out.println("PASS " + name);
        passed++;
    }

    private static void reject(String name, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { check(name, true); return; }
        throw new AssertionError(name + " should be rejected");
    }

    private static Clock at(String hotelTime) {
        return Clock.fixed(LocalDateTime.parse(hotelTime).atZone(Hotel_2703142.ZONE).toInstant(), Hotel_2703142.ZONE);
    }

    private static Room_2703142 room(String type, String id) {
        return Room_2703142.create(type, id, Room_2703142.Bed.Double, 2, Room_2703142.Status.Available);
    }

    private static boolean amount(BigDecimal actual, String expected) {
        return actual.compareTo(new BigDecimal(expected)) == 0;
    }

    public static void main(String[] args) throws Exception {
        Clock clock = at("2030-01-01T10:00");
        Hotel_2703142 hotel = new Hotel_2703142(clock);
        Guest_2703142 guest = new Guest_2703142("P1", "Test Guest", "+94 777 000 000", "guest@example.test");
        hotel.register(guest);
        check("Guest lookup", hotel.guest("p1") == guest);
        reject("Duplicate passport", () -> hotel.register(guest));
        reject("Invalid email", () -> hotel.register(new Guest_2703142("P2", "Guest", "1234567", "bad")));
        reject("Invalid contact", () -> hotel.register(new Guest_2703142("P2", "Guest", "bad", "a@b.co")));
        reject("Punctuation-only ID", () -> hotel.register(new Guest_2703142("---", "Guest", "1234567", "a@b.co")));
        reject("Misplaced plus signs", () -> hotel.register(new Guest_2703142("AB123", "Guest", "12++34567", "a@b.co")));
        reject("Empty email domain label", () -> hotel.register(new Guest_2703142("AB123", "Guest", "1234567", "test@host..com")));
        Guest_2703142 local = new Guest_2703142("912345678V", "N. Perera", "077 123 4567", "n.perera@example.lk");
        Guest_2703142 overseas = new Guest_2703142("AB123", "María O'Neil", "+1 (212) 555-0198", "maria+trip@example.co.uk");
        hotel.register(local);
        hotel.register(overseas);
        check("Local guest registration", hotel.guest(local.document) == local);
        check("International guest registration", hotel.guest(overseas.document) == overseas);
        String[] types = {"Standard", "Deluxe", "Suite", "Family Suite"};
        int[] rates = {6000, 9000, 14000, 18000}, extras = {1000, 1500, 2000, 2500}, taxes = {10, 12, 15, 15};
        for (int i = 0; i < types.length; i++) {
            Room_2703142 room = room(types[i], "R" + i);
            hotel.addRoom(room);
            check(types[i] + " package", room.dailyRate() == rates[i] && room.extraGuestRate() == extras[i]
                    && room.taxPercent() == taxes[i] && room.includesBreakfast() == (i > 0));
        }
        reject("Duplicate room", () -> hotel.addRoom(room("Suite", "R0")));
        hotel.updateRoom(room("Suite", "R1"));
        check("Room update", hotel.room("R1").type().equals("Suite"));
        hotel.removeRoom("R1");
        check("Remove and list rooms", hotel.allRooms().size() == 3);
        LocalDate start = LocalDate.of(2030, 1, 3);
        LocalDate end = start.plusDays(5);
        for (Room_2703142.Status roomStatus : new Room_2703142.Status[]{Room_2703142.Status.Reserved, Room_2703142.Status.UnderMaintenance}) {
            hotel.changeStatus("R0", roomStatus);
            reject(roomStatus + " booking blocked", () -> hotel.reserve("P1", "R0", start, end, 2));
        }
        hotel.changeStatus("R0", Room_2703142.Status.Available);
        reject("One day notice", () -> hotel.reserve("P1", "R0", start.minusDays(1), end, 2));
        reject("Zero nights", () -> hotel.reserve("P1", "R0", start, start, 2));
        reject("Zero guests", () -> hotel.reserve("P1", "R0", start, end, 0));
        Reservation_2703142 booking = hotel.reserve("P1", "R0", start, end, 3);
        check("Two days notice accepted", hotel.booking(booking.id) == booking);
        reject("Overlap", () -> hotel.reserve("P1", "R0", end.minusDays(1), end.plusDays(1), 2));
        reject("Booked room removal", () -> hotel.removeRoom("R0"));
        check("Date and type search", hotel.searchRooms("Standard", start, end).isEmpty()
                && hotel.searchRooms("Suite", start, end).size() == 1);
        Reservation_2703142 nextBooking = hotel.reserve("P1", "R0", end, end.plusDays(4), 2);
        check("Checkout day available", !nextBooking.id.equals(booking.id));
        LongStayDiscount_2703142 policy = new LongStayDiscount_2703142();
        Invoice_2703142 bill = new Invoice_2703142(booking, policy);
        check("Five nights, one extra guest, base-only discount, tax and advance",
                amount(bill.base, "30000") && amount(bill.discount, "3600") && amount(bill.extra, "5000")
                        && amount(bill.tax, "3140") && amount(bill.balance, "24540"));
        Invoice_2703142 shortBill = new Invoice_2703142(nextBooking, policy);
        check("Four nights at capacity", amount(shortBill.discount, "0") && amount(shortBill.extra, "0"));
        check("Guest name and ID searches", hotel.byName("test guest").size() == 2 && hotel.byName("missing").isEmpty());
        reject("Unknown booking ID", () -> hotel.booking("X"));
        check("Filter and sort by check-in", hotel.byDate(end).size() == 1 && hotel.byDate(null).get(0) == booking);
        reject("Conflicting modification", () -> hotel.modify(booking.id, "R0", start, end.plusDays(1), 2));
        check("Failed edit keeps original", hotel.booking(booking.id) == booking);
        Path dir = Files.createTempDirectory("smartstay-test");
        Path file = dir.resolve("hotel.dat");
        try {
            Storage_2703142.save(file, hotel);
            Hotel_2703142 boundary = Storage_2703142.load(file, at("2030-01-02T14:00"));
            check("Saved invoice after restart", amount(new Invoice_2703142(boundary.booking(booking.id), policy).balance, "24540"));
            boundary.modify(booking.id, "R0", start, start.plusDays(1), 2);
            check("Exactly 24 hours: edit and single advance", amount(new Invoice_2703142(boundary.booking(booking.id), policy).balance, "-3400"));
            boundary.cancel(booking.id);
            check("Exactly 24 hours: cancellation frees dates", boundary.searchRooms("Standard", start, end).size() == 1);
            reject("Repeated refund prevented", () -> boundary.cancel(booking.id));
            Hotel_2703142 late = Storage_2703142.load(file, at("2030-01-02T14:01"));
            reject("Inside 24 hours: no cancellation", () -> late.cancel(booking.id));
            reject("Inside 24 hours: no edit", () -> late.modify(booking.id, "R0", start, end, 1));
            Storage_2703142.save(file, boundary);
            check("Refund status after restart", Storage_2703142.load(file, clock).booking(booking.id).cancelled());
            Hotel_2703142 timed = new Hotel_2703142(clock);
            timed.register(guest);
            timed.addRoom(room("Standard", "T1"));
            Reservation_2703142 timedBooking = timed.reserve("P1", "T1", start, end, 2, LocalTime.of(15, 30));
            Storage_2703142.save(file, timed);
            Hotel_2703142 atDeadline = Storage_2703142.load(file, at("2030-01-02T15:30"));
            check("Custom check-in time survives restart", atDeadline.booking(timedBooking.id).checkInTime.equals(LocalTime.of(15, 30)));
            atDeadline.modify(timedBooking.id, "T1", start, end, 2, LocalTime.of(16, 0));
            check("Time modification at original deadline", atDeadline.booking(timedBooking.id).checkInTime.equals(LocalTime.of(16, 0)));
            Hotel_2703142 cancelAtDeadline = Storage_2703142.load(file, at("2030-01-02T15:30"));
            cancelAtDeadline.cancel(timedBooking.id);
            check("Custom time cancellation exactly 24 hours", cancelAtDeadline.booking(timedBooking.id).cancelled());
            Hotel_2703142 afterDeadline = Storage_2703142.load(file, at("2030-01-02T15:31"));
            reject("Custom time cancellation inside 24 hours", () -> afterDeadline.cancel(timedBooking.id));
            reject("Custom time edit inside 24 hours", () -> afterDeadline.modify(timedBooking.id, "T1", start, end, 2, LocalTime.of(17, 0)));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
        System.out.println("Passed " + passed + " checks.");
    }
}
