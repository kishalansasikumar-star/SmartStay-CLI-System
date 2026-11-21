package smartstay.model;

public class StandardRoom extends Room {
    public StandardRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected RoomPackage roomPackage() {
        return RoomPackage.STANDARD;
    }
}
