package smartstay.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class E245549_Reservation {
    public static final int ADVANCE = 10000;
    public final String id;
    public final E245549_Guest guest;
    public final E245549_Room room;
    public final LocalDate arrival, departure;
    public final int guests;
    public final LocalTime checkInTime;
    private boolean cancelled;

    public E245549_Reservation(String id, E245549_Guest guest, E245549_Room room,
                               LocalDate arrival, LocalDate departure, int guests) {
        this(id, guest, room, arrival, departure, guests, LocalTime.of(14, 0));
    }

    public E245549_Reservation(String id, E245549_Guest guest, E245549_Room room,
                               LocalDate arrival, LocalDate departure, int guests, LocalTime checkInTime) {
        this.checkInTime = java.util.Objects.requireNonNull(checkInTime, "Check-in time is required.");
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.arrival = arrival;
        this.departure = departure;
        this.guests = guests;
    }

    public boolean cancelled() { return cancelled; }
    public void cancel() { cancelled = true; }

    public boolean overlaps(LocalDate start, LocalDate end) {
        // Checkout day is free for the next guest.
        return !cancelled && start.isBefore(departure) && arrival.isBefore(end);
    }
}
