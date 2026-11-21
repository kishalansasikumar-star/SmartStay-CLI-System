package smartstay.model;

public class FamilySuiteRoom extends Room {
    public FamilySuiteRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected RoomPackage roomPackage() {
        return RoomPackage.FAMILY_SUITE;
    }
}
