package smartstay.model;

public class E245549_SuiteRoom extends E245549_Room {
    public E245549_SuiteRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override public String type() { return "Suite"; }
    @Override public int dailyRate() { return 14000; }
    @Override public int extraGuestRate() { return 2000; }
    @Override public int taxPercent() { return 15; }
    @Override public boolean includesBreakfast() { return true; }
}
