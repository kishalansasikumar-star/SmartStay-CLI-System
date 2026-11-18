package smartstay.model;

public class E245549_DeluxeRoom extends E245549_Room {
    public E245549_DeluxeRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override public String type() { return "Deluxe"; }
    @Override public int dailyRate() { return 9000; }
    @Override public int extraGuestRate() { return 1500; }
    @Override public int taxPercent() { return 12; }
    @Override public boolean includesBreakfast() { return true; }
}
