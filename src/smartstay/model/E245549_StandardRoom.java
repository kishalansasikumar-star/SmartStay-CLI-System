package smartstay.model;

public class E245549_StandardRoom extends E245549_Room {
    public E245549_StandardRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override public String type() { return "Standard"; }
    @Override public int dailyRate() { return 6000; }
    @Override public int extraGuestRate() { return 1000; }
    @Override public int taxPercent() { return 10; }
    @Override public boolean includesBreakfast() { return false; }
}
