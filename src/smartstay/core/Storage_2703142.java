package smartstay.core;

import smartstay.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

// Store plain field values, including booked room details for historical invoices.
public class Storage_2703142 {
    private static final String FORMAT = "SmartStay-2";

    private static void writeRoom(DataOutputStream out, Room_2703142 room) throws IOException {
        out.writeUTF(room.type());
        out.writeUTF(room.id);
        out.writeUTF(room.bed.name());
        out.writeInt(room.capacity);
        out.writeUTF(room.status().name());
    }

    private static Room_2703142 readRoom(DataInputStream in) throws IOException {
        return Room_2703142.create(in.readUTF(), in.readUTF(), Room_2703142.Bed.valueOf(in.readUTF()),
                in.readInt(), Room_2703142.Status.valueOf(in.readUTF()));
    }

    public static void save(Path file, Hotel_2703142 hotel) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
            out.writeUTF(FORMAT);
            out.writeInt(hotel.rooms.size());
            for (Room_2703142 room : hotel.rooms.values()) writeRoom(out, room);
            out.writeInt(hotel.guests.size());
            for (Guest_2703142 guest : hotel.guests.values()) {
                out.writeUTF(guest.document);
                out.writeUTF(guest.name);
                out.writeUTF(guest.phone);
                out.writeUTF(guest.email);
            }
            out.writeInt(hotel.bookings.size());
            for (Reservation_2703142 booking : hotel.bookings.values()) {
                out.writeUTF(booking.id);
                out.writeUTF(booking.guest.document);
                writeRoom(out, booking.room);
                out.writeUTF(booking.arrival.toString());
                out.writeUTF(booking.departure.toString());
                out.writeInt(booking.guests);
                out.writeBoolean(booking.cancelled());
                out.writeUTF(booking.checkInTime.toString());
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Hotel_2703142 load(Path file, Clock clock) throws IOException {
        Hotel_2703142 hotel = new Hotel_2703142(clock);
        if (Files.notExists(file)) return hotel;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            String format = in.readUTF();
            if (!FORMAT.equals(format) && !"SmartStay-1".equals(format)) throw new IOException("Unrecognised data file.");
            int rooms = in.readInt();
            for (int i = 0; i < rooms; i++) hotel.addRoom(readRoom(in));
            int guests = in.readInt();
            for (int i = 0; i < guests; i++)
                // Saved registrations can predate the current input rules.
                hotel.addGuestRecord(new Guest_2703142(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()));
            int bookings = in.readInt();
            for (int i = 0; i < bookings; i++) {
                String id = in.readUTF();
                Guest_2703142 guest = hotel.guest(in.readUTF());
                Room_2703142 room = readRoom(in);
                LocalDate arrival = LocalDate.parse(in.readUTF());
                LocalDate departure = LocalDate.parse(in.readUTF());
                int count = in.readInt();
                boolean cancelled = in.readBoolean();
                LocalTime time = FORMAT.equals(format) ? LocalTime.parse(in.readUTF()) : LocalTime.of(14, 0);
                Reservation_2703142 booking = new Reservation_2703142(id, guest, room, arrival, departure, count, time);
                if (cancelled) booking.cancel();
                hotel.bookings.put(id, booking);
            }
        }
        return hotel;
    }
}
