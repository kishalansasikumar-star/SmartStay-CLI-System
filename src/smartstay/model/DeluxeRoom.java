package smartstay.model;

public class DeluxeRoom extends Room {
    public DeluxeRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected RoomPackage roomPackage() {
        return RoomPackage.DELUXE;
    }
}

