import smartstay.core.*;
import smartstay.model.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;

public class E245549_SystemTest {
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
        return Clock.fixed(LocalDateTime.parse(hotelTime).atZone(E245549_Hotel.ZONE).toInstant(), E245549_Hotel.ZONE);
    }

    private static E245549_Room room(String type, String id) {
        return E245549_Room.create(type, id, E245549_Room.Bed.Double, 2, E245549_Room.Status.Available);
    }

    private static boolean amount(BigDecimal actual, String expected) {
        return actual.compareTo(new BigDecimal(expected)) == 0;
    }

    public static void main(String[] args) throws Exception {
        Clock clock = at("2030-01-01T10:00");
        E245549_Hotel hotel = new E245549_Hotel(clock);
        E245549_Guest guest = new E245549_Guest("P1", "Test Guest", "+94 777 000 000", "guest@example.test");
        hotel.register(guest);
        check("Guest lookup", hotel.guest("p1") == guest);
        reject("Duplicate passport", () -> hotel.register(guest));
        reject("Invalid email", () -> hotel.register(new E245549_Guest("P2", "Guest", "1234567", "bad")));
        reject("Invalid contact", () -> hotel.register(new E245549_Guest("P2", "Guest", "bad", "a@b.co")));
        reject("Punctuation-only ID", () -> hotel.register(new E245549_Guest("---", "Guest", "1234567", "a@b.co")));
        reject("Misplaced plus signs", () -> hotel.register(new E245549_Guest("AB123", "Guest", "12++34567", "a@b.co")));
        reject("Empty email domain label", () -> hotel.register(new E245549_Guest("AB123", "Guest", "1234567", "test@host..com")));
        E245549_Guest local = new E245549_Guest("912345678V", "N. Perera", "077 123 4567", "n.perera@example.lk");
        E245549_Guest overseas = new E245549_Guest("AB123", "María O'Neil", "+1 (212) 555-0198", "maria+trip@example.co.uk");
        hotel.register(local);
        hotel.register(overseas);
        check("Local guest registration", hotel.guest(local.document) == local);
        check("International guest registration", hotel.guest(overseas.document) == overseas);
        String[] types = {"Standard", "Deluxe", "Suite", "Family Suite"};
        int[] rates = {6000, 9000, 14000, 18000}, extras = {1000, 1500, 2000, 2500}, taxes = {10, 12, 15, 15};
        for (int i = 0; i < types.length; i++) {
            E245549_Room r = room(types[i], "R" + i);
            hotel.addRoom(r);
            check(types[i] + " package", r.dailyRate() == rates[i] && r.extraGuestRate() == extras[i]
                    && r.taxPercent() == taxes[i] && r.includesBreakfast() == (i > 0));
        }
        reject("Duplicate room", () -> hotel.addRoom(room("Suite", "R0")));
        hotel.updateRoom(room("Suite", "R1"));
        check("Room update", hotel.room("R1").type().equals("Suite"));
        hotel.removeRoom("R1");
        check("Remove and list rooms", hotel.allRooms().size() == 3);
        LocalDate start = LocalDate.of(2030, 1, 3);
        LocalDate end = start.plusDays(5);
        for (E245549_Room.Status s : new E245549_Room.Status[]{E245549_Room.Status.Reserved, E245549_Room.Status.UnderMaintenance}) {
            hotel.changeStatus("R0", s);
            reject(s + " booking blocked", () -> hotel.reserve("P1", "R0", start, end, 2));
        }
        hotel.changeStatus("R0", E245549_Room.Status.Available);
        reject("One day notice", () -> hotel.reserve("P1", "R0", start.minusDays(1), end, 2));
        reject("Zero nights", () -> hotel.reserve("P1", "R0", start, start, 2));
        reject("Zero guests", () -> hotel.reserve("P1", "R0", start, end, 0));
        E245549_Reservation b = hotel.reserve("P1", "R0", start, end, 3);
        check("Two days notice accepted", hotel.booking(b.id) == b);
        reject("Overlap", () -> hotel.reserve("P1", "R0", end.minusDays(1), end.plusDays(1), 2));
        reject("Booked room removal", () -> hotel.removeRoom("R0"));
        check("Date and type search", hotel.searchRooms("Standard", start, end).isEmpty()
                && hotel.searchRooms("Suite", start, end).size() == 1);
        E245549_Reservation next = hotel.reserve("P1", "R0", end, end.plusDays(4), 2);
        check("Checkout day available", !next.id.equals(b.id));
        E245549_LongStayDiscount policy = new E245549_LongStayDiscount();
        E245549_Invoice bill = new E245549_Invoice(b, policy);
        check("Five nights, one extra guest, base-only discount, tax and advance",
                amount(bill.base, "30000") && amount(bill.discount, "3600") && amount(bill.extra, "1000")
                        && amount(bill.tax, "2740") && amount(bill.balance, "20140"));
        E245549_Invoice shortBill = new E245549_Invoice(next, policy);
        check("Four nights at capacity", amount(shortBill.discount, "0") && amount(shortBill.extra, "0"));
        check("Guest name and ID searches", hotel.byName("test guest").size() == 2 && hotel.byName("missing").isEmpty());
        reject("Unknown booking ID", () -> hotel.booking("X"));
        check("Filter and sort by check-in", hotel.byDate(end).size() == 1 && hotel.byDate(null).get(0) == b);
        reject("Conflicting modification", () -> hotel.modify(b.id, "R0", start, end.plusDays(1), 2));
        check("Failed edit keeps original", hotel.booking(b.id) == b);
        Path dir = Files.createTempDirectory("smartstay-test");
        Path file = dir.resolve("hotel.dat");
        try {
            E245549_Storage.save(file, hotel);
            E245549_Hotel boundary = E245549_Storage.load(file, at("2030-01-02T14:00"));
            check("Saved invoice after restart", amount(new E245549_Invoice(boundary.booking(b.id), policy).balance, "20140"));
            boundary.modify(b.id, "R0", start, start.plusDays(1), 2);
            check("Exactly 24 hours: edit and single advance", amount(new E245549_Invoice(boundary.booking(b.id), policy).balance, "-3400"));
            boundary.cancel(b.id);
            check("Exactly 24 hours: cancellation frees dates", boundary.searchRooms("Standard", start, end).size() == 1);
            reject("Repeated refund prevented", () -> boundary.cancel(b.id));
            E245549_Hotel late = E245549_Storage.load(file, at("2030-01-02T14:01"));
            reject("Inside 24 hours: no cancellation", () -> late.cancel(b.id));
            reject("Inside 24 hours: no edit", () -> late.modify(b.id, "R0", start, end, 1));
            E245549_Storage.save(file, boundary);
            check("Refund status after restart", E245549_Storage.load(file, clock).booking(b.id).cancelled());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
        System.out.println("Passed " + passed + " checks.");
    }
}
