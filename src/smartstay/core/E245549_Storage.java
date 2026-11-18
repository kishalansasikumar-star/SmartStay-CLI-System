package smartstay.core;

import smartstay.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

// Store plain field values, including booked room details for historical invoices.
public class E245549_Storage {
    private static final String FORMAT = "SmartStay-1";

    private static void writeRoom(DataOutputStream out, E245549_Room r) throws IOException {
        out.writeUTF(r.type());
        out.writeUTF(r.id);
        out.writeUTF(r.bed.name());
        out.writeInt(r.capacity);
        out.writeUTF(r.status().name());
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
            for (E245549_Guest g : hotel.guests.values()) {
                out.writeUTF(g.document);
                out.writeUTF(g.name);
                out.writeUTF(g.phone);
                out.writeUTF(g.email);
            }
            out.writeInt(hotel.bookings.size());
            for (E245549_Reservation b : hotel.bookings.values()) {
                out.writeUTF(b.id);
                out.writeUTF(b.guest.document);
                writeRoom(out, b.room);
                out.writeUTF(b.arrival.toString());
                out.writeUTF(b.departure.toString());
                out.writeInt(b.guests);
                out.writeBoolean(b.cancelled());
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static E245549_Hotel load(Path file, Clock clock) throws IOException {
        E245549_Hotel hotel = new E245549_Hotel(clock);
        if (Files.notExists(file)) return hotel;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            if (!FORMAT.equals(in.readUTF())) throw new IOException("Unrecognised data file.");
            int rooms = in.readInt();
            for (int i = 0; i < rooms; i++) hotel.addRoom(readRoom(in));
            int guests = in.readInt();
            for (int i = 0; i < guests; i++)
                hotel.register(new E245549_Guest(in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()));
            int bookings = in.readInt();
            for (int i = 0; i < bookings; i++) {
                String id = in.readUTF();
                E245549_Guest guest = hotel.guest(in.readUTF());
                E245549_Reservation b = new E245549_Reservation(id, guest, readRoom(in),
                        LocalDate.parse(in.readUTF()), LocalDate.parse(in.readUTF()), in.readInt());
                if (in.readBoolean()) b.cancel();
                hotel.bookings.put(id, b);
            }
        }
        return hotel;
    }
}
