package smartstay.model;

public class SuiteRoom extends Room {
    public SuiteRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected RoomPackage roomPackage() {
        return RoomPackage.SUITE;
    }
}
