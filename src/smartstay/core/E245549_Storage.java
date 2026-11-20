package smartstay.core;

import smartstay.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

// Store plain field values, including booked room details for historical invoices.
public class E245549_Storage {
    private static final String FORMAT = "SmartStay-2";

    private static void writeRoom(DataOutputStream out, E245549_Room room) throws IOException {
        out.writeUTF(room.type());
        out.writeUTF(room.id);
        out.writeUTF(room.bed.name());
        out.writeInt(room.capacity);
        out.writeUTF(room.status().name());
    }

    private static E245549_Room readRoom(DataInputStream in) throws IOException {
        return E245549_Room.create(in.readUTF(), in.readUTF(), E245549_Room.Bed.valueOf(in.readUTF()),
                in.readInt(), E245549_Room.Status.valueOf(in.readUTF()));
    }

    public static void save(Path file, E245549_Hotel hotel) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
            out.writeUTF(FORMAT);
            out.writeInt(hotel.rooms.size());
            for (E245549_Room room : hotel.rooms.values()) writeRoom(out, room);
            out.writeInt(hotel.guests.size());
            for (E245549_Guest guest : hotel.guests.values()) {
                out.writeUTF(guest.document);
                out.writeUTF(guest.name);
                out.writeUTF(guest.phone);
                out.writeUTF(guest.email);
            }
            out.writeInt(hotel.bookings.size());
            for (E245549_Reservation booking : hotel.bookings.values()) {
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

    public static E245549_Hotel load(Path file, Clock clock) throws IOException {
        E245549_Hotel hotel = new E245549_Hotel(clock);
        if (Files.notExists(file)) return hotel;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            String format = in.readUTF();
            if (!FORMAT.equals(format) && !"SmartStay-1".equals(format)) throw new IOException("Unrecognised data file.");
            int rooms = in.readInt();
            for (int i = 0; i < rooms; i++) hotel.addRoom(readRoom(in));
            int guests = in.readInt();
            for (int i = 0; i < guests; i++)
                // Saved registrations can predate the current input rules.
                hotel.addGuestRecord(new E245549_Guest(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()));
            int bookings = in.readInt();
            for (int i = 0; i < bookings; i++) {
                String id = in.readUTF();
                E245549_Guest guest = hotel.guest(in.readUTF());
                E245549_Room room = readRoom(in);
                LocalDate arrival = LocalDate.parse(in.readUTF());
                LocalDate departure = LocalDate.parse(in.readUTF());
                int count = in.readInt();
                boolean cancelled = in.readBoolean();
                LocalTime time = FORMAT.equals(format) ? LocalTime.parse(in.readUTF()) : LocalTime.of(14, 0);
                E245549_Reservation booking = new E245549_Reservation(id, guest, room, arrival, departure, count, time);
                if (cancelled) booking.cancel();
                hotel.bookings.put(id, booking);
            }
        }
        return hotel;
    }
}
