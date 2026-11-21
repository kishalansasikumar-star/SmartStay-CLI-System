package smartstay.core;

import smartstay.model.*;
import java.time.*;
import java.util.*;

public class Hotel_2703142 {
    // Maps preserve entry order for listings and allow direct lookup by identifier.
    final Map<String, Room_2703142> rooms = new LinkedHashMap<>();
    final Map<String, Guest_2703142> guests = new LinkedHashMap<>();
    final Map<String, Reservation_2703142> bookings = new LinkedHashMap<>();
    private final Clock clock;
    public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    public Hotel_2703142(Clock clock) { this.clock = clock.withZone(ZONE); }
    private String key(String value) { return value.trim().toUpperCase(Locale.ROOT); }

    public void register(Guest_2703142 guest) {
        guest.validateForRegistration();
        addGuestRecord(guest);
    }

    void addGuestRecord(Guest_2703142 guest) {
        if (guests.putIfAbsent(key(guest.document), guest) != null)
            throw new IllegalArgumentException("NIC/passport already registered.");
    }

    public Guest_2703142 guest(String document) {
        Guest_2703142 guest = guests.get(key(document));
        if (guest == null) throw new IllegalArgumentException("Guest not found. Register first.");
        return guest;
    }

    public void addRoom(Room_2703142 room) {
        if (rooms.putIfAbsent(room.id, room) != null) throw new IllegalArgumentException("Room ID already exists.");
    }

    public Room_2703142 room(String id) {
        Room_2703142 room = rooms.get(key(id));
        if (room == null) throw new IllegalArgumentException("Room not found.");
        return room;
    }

    public Collection<Room_2703142> allRooms() { return Collections.unmodifiableCollection(rooms.values()); }

    private void requireEditableRoom(String id) {
        room(id);
        for (Reservation_2703142 booking : bookings.values()) {
            if (booking.room.id.equals(key(id)) && !booking.cancelled() && booking.departure.isAfter(LocalDate.now(clock)))
                throw new IllegalArgumentException("This room has a current or future booking.");
        }
    }

    public void updateRoom(Room_2703142 replacement) {
        requireEditableRoom(replacement.id);
        rooms.put(replacement.id, replacement);
    }

    public void removeRoom(String id) {
        requireEditableRoom(id);
        rooms.remove(key(id));
    }

    public void changeStatus(String id, Room_2703142.Status status) {
        requireEditableRoom(id);
        room(id).setStatus(status);
    }

    public void checkBookingNotice(LocalDate start) {
        if (start.isBefore(LocalDate.now(clock).plusDays(2)))
            throw new IllegalArgumentException("Book at least two calendar days before check-in.");
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("Checkout must follow check-in.");
    }

    public Room_2703142.Status statusFor(Room_2703142 room, LocalDate start, LocalDate end, String ignoreId) {
        if (room.status() != Room_2703142.Status.Available) return room.status();
        for (Reservation_2703142 booking : bookings.values()) {
            if (!booking.id.equals(ignoreId) && booking.room.id.equals(room.id) && booking.overlaps(start, end))
                return Room_2703142.Status.Reserved;
        }
        return Room_2703142.Status.Available;
    }

    public Room_2703142.Status statusToday(Room_2703142 room) {
        LocalDate today = LocalDate.now(clock);
        return statusFor(room, today, today.plusDays(1), "");
    }

    public List<Room_2703142> searchRooms(String type, LocalDate start, LocalDate end) {
        checkDates(start, end);
        List<Room_2703142> found = new ArrayList<>();
        for (Room_2703142 room : rooms.values()) {
            if (room.type().equals(type) && statusFor(room, start, end, "") == Room_2703142.Status.Available)
                found.add(room);
        }
        return found;
    }

    public Reservation_2703142 booking(String id) {
        Reservation_2703142 booking = bookings.get(key(id));
        if (booking == null) throw new IllegalArgumentException("Booking ID not found.");
        return booking;
    }

    public List<Reservation_2703142> byName(String name) {
        List<Reservation_2703142> result = new ArrayList<>();
        for (Reservation_2703142 booking : bookings.values()) {
            if (booking.guest.name.equalsIgnoreCase(name.trim())) result.add(booking);
        }
        return result;
    }

    public List<Reservation_2703142> byDate(LocalDate date) {
        List<Reservation_2703142> result = new ArrayList<>();
        for (Reservation_2703142 booking : bookings.values()) {
            if (date == null || booking.arrival.equals(date)) result.add(booking);
        }
        result.sort(Comparator.comparing(booking -> booking.arrival));
        return result;
    }

    private Reservation_2703142 saveBooking(String id, Guest_2703142 guest, String roomId,
                                            LocalDate start, LocalDate end, int count, LocalTime checkInTime) {
        checkDates(start, end);
        Reservation_2703142 original = bookings.get(id);
        boolean newArrival = original == null || !start.equals(original.arrival);
        if (newArrival) checkBookingNotice(start);
        if (count <= 0) throw new IllegalArgumentException("Guest count must be positive.");
        Room_2703142 room = room(roomId);
        if (statusFor(room, start, end, id) != Room_2703142.Status.Available)
            throw new IllegalArgumentException("Room unavailable for these dates.");
        Reservation_2703142 booking = new Reservation_2703142(id, guest, room, start, end, count, checkInTime);
        bookings.put(id, booking);
        return booking;
    }

    public Reservation_2703142 reserve(String document, String roomId, LocalDate start, LocalDate end, int count) {
        return reserve(document, roomId, start, end, count, LocalTime.of(14, 0));
    }

    public Reservation_2703142 reserve(String document, String roomId, LocalDate start, LocalDate end, int count, LocalTime checkInTime) {
        String id = String.format(Locale.ROOT, "B%04d", bookings.size() + 1);
        return saveBooking(id, guest(document), roomId, start, end, count, checkInTime);
    }

    private Reservation_2703142 editableBooking(String id) {
        Reservation_2703142 booking = booking(id);
        if (booking.cancelled()) throw new IllegalArgumentException("Booking already cancelled.");
        Instant cutoff = booking.arrival.atTime(booking.checkInTime).atZone(ZONE).minusHours(24).toInstant();
        if (clock.instant().isAfter(cutoff)) throw new IllegalArgumentException("24-hour change deadline has passed.");
        return booking;
    }

    public Reservation_2703142 modify(String id, String roomId, LocalDate start, LocalDate end, int count) {
        return modify(id, roomId, start, end, count, booking(id).checkInTime);
    }

    public Reservation_2703142 modify(String id, String roomId, LocalDate start, LocalDate end, int count, LocalTime checkInTime) {
        Reservation_2703142 existingBooking = editableBooking(id);
        return saveBooking(existingBooking.id, existingBooking.guest, roomId, start, end, count, checkInTime);
    }

    public void cancel(String id) { editableBooking(id).cancel(); }
}
