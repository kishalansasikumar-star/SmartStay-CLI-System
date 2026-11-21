package smartstay.model;

public class FamilySuiteRoom_2703142 extends Room_2703142 {
    public FamilySuiteRoom_2703142(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected RoomPackage_2703142 roomPackage() {
        return RoomPackage_2703142.FAMILY_SUITE;
    }
}
