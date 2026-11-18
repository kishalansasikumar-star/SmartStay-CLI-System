package smartstay.core;

import smartstay.model.*;
import java.time.*;
import java.util.*;

public class E245549_Hotel {
    // Maps preserve entry order for listings and allow direct lookup by identifier.
    final Map<String, E245549_Room> rooms = new LinkedHashMap<>();
    final Map<String, E245549_Guest> guests = new LinkedHashMap<>();
    final Map<String, E245549_Reservation> bookings = new LinkedHashMap<>();
    private final Clock clock;
    public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    public E245549_Hotel(Clock clock) { this.clock = clock.withZone(ZONE); }
    private String key(String value) { return value.trim().toUpperCase(Locale.ROOT); }

    public void register(E245549_Guest guest) {
        if (guests.putIfAbsent(key(guest.document), guest) != null)
            throw new IllegalArgumentException("NIC/passport already registered.");
    }

    public E245549_Guest guest(String document) {
        E245549_Guest guest = guests.get(key(document));
        if (guest == null) throw new IllegalArgumentException("Guest not found. Register first.");
        return guest;
    }

    public void addRoom(E245549_Room room) {
        if (rooms.putIfAbsent(room.id, room) != null) throw new IllegalArgumentException("Room ID already exists.");
    }

    public E245549_Room room(String id) {
        E245549_Room room = rooms.get(key(id));
        if (room == null) throw new IllegalArgumentException("Room not found.");
        return room;
    }

    public Collection<E245549_Room> allRooms() { return Collections.unmodifiableCollection(rooms.values()); }

    private void requireEditableRoom(String id) {
        room(id);
        for (E245549_Reservation b : bookings.values()) {
            if (b.room.id.equals(key(id)) && !b.cancelled() && b.departure.isAfter(LocalDate.now(clock)))
                throw new IllegalArgumentException("This room has a current or future booking.");
        }
    }

    public void updateRoom(E245549_Room replacement) {
        requireEditableRoom(replacement.id);
        rooms.put(replacement.id, replacement);
    }

    public void removeRoom(String id) {
        requireEditableRoom(id);
        rooms.remove(key(id));
    }

    public void changeStatus(String id, E245549_Room.Status status) {
        requireEditableRoom(id);
        room(id).setStatus(status);
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("Checkout must follow check-in.");
    }

    public E245549_Room.Status statusFor(E245549_Room room, LocalDate start, LocalDate end, String ignoreId) {
        if (room.status() != E245549_Room.Status.Available) return room.status();
        for (E245549_Reservation b : bookings.values()) {
            if (!b.id.equals(ignoreId) && b.room.id.equals(room.id) && b.overlaps(start, end))
                return E245549_Room.Status.Reserved;
        }
        return E245549_Room.Status.Available;
    }

    public E245549_Room.Status statusToday(E245549_Room room) {
        LocalDate today = LocalDate.now(clock);
        return statusFor(room, today, today.plusDays(1), "");
    }

    public List<E245549_Room> searchRooms(String type, LocalDate start, LocalDate end) {
        checkDates(start, end);
        List<E245549_Room> found = new ArrayList<>();
        for (E245549_Room room : rooms.values()) {
            if (room.type().equals(type) && statusFor(room, start, end, "") == E245549_Room.Status.Available)
                found.add(room);
        }
        return found;
    }

    public E245549_Reservation booking(String id) {
        E245549_Reservation booking = bookings.get(key(id));
        if (booking == null) throw new IllegalArgumentException("Booking ID not found.");
        return booking;
    }

    public List<E245549_Reservation> byName(String name) {
        List<E245549_Reservation> result = new ArrayList<>();
        for (E245549_Reservation b : bookings.values()) {
            if (b.guest.name.equalsIgnoreCase(name.trim())) result.add(b);
        }
        return result;
    }

    public List<E245549_Reservation> byDate(LocalDate date) {
        List<E245549_Reservation> result = new ArrayList<>();
        for (E245549_Reservation b : bookings.values()) {
            if (date == null || b.arrival.equals(date)) result.add(b);
        }
        result.sort(Comparator.comparing(b -> b.arrival));
        return result;
    }

    private E245549_Reservation saveBooking(String id, E245549_Guest guest, String roomId,
                                            LocalDate start, LocalDate end, int count) {
        checkDates(start, end);
        E245549_Reservation original = bookings.get(id);
        boolean newArrival = original == null || !start.equals(original.arrival);
        if (newArrival && start.isBefore(LocalDate.now(clock).plusDays(2)))
            throw new IllegalArgumentException("Book at least two calendar days before check-in.");
        if (count <= 0) throw new IllegalArgumentException("Guest count must be positive.");
        E245549_Room room = room(roomId);
        if (statusFor(room, start, end, id) != E245549_Room.Status.Available)
            throw new IllegalArgumentException("Room unavailable for these dates.");
        E245549_Reservation b = new E245549_Reservation(id, guest, room, start, end, count);
        bookings.put(id, b);
        return b;
    }

    public E245549_Reservation reserve(String document, String roomId, LocalDate start, LocalDate end, int count) {
        String id = String.format(Locale.ROOT, "B%04d", bookings.size() + 1);
        return saveBooking(id, guest(document), roomId, start, end, count);
    }

    private E245549_Reservation editableBooking(String id) {
        E245549_Reservation b = booking(id);
        if (b.cancelled()) throw new IllegalArgumentException("Booking already cancelled.");
        Instant cutoff = b.arrival.atTime(14, 0).atZone(ZONE).minusHours(24).toInstant();
        if (clock.instant().isAfter(cutoff)) throw new IllegalArgumentException("24-hour change deadline has passed.");
        return b;
    }

    public E245549_Reservation modify(String id, String roomId, LocalDate start, LocalDate end, int count) {
        E245549_Reservation old = editableBooking(id);
        return saveBooking(old.id, old.guest, roomId, start, end, count);
    }

    public void cancel(String id) { editableBooking(id).cancel(); }
}
