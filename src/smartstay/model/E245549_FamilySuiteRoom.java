package smartstay.model;

public class E245549_FamilySuiteRoom extends E245549_Room {
    public E245549_FamilySuiteRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override public String type() { return "Family Suite"; }
    @Override public int dailyRate() { return 18000; }
    @Override public int extraGuestRate() { return 2500; }
    @Override public int taxPercent() { return 15; }
    @Override public boolean includesBreakfast() { return true; }
}
