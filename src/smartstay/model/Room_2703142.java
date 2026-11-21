package smartstay.model;

public abstract class Room_2703142 {
    public enum Bed { Single, Double, Queen, King }
    public enum Status { Available, Reserved, UnderMaintenance }

    public final String id;
    public final Bed bed;
    public final int capacity;
    private Status status;

    protected Room_2703142(String id, Bed bed, int capacity, Status status) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive.");
        if (!id.matches("[A-Za-z0-9-]+")) throw new IllegalArgumentException("Use letters, digits or hyphens for the room ID.");
        this.id = id.toUpperCase(java.util.Locale.ROOT);
        this.bed = bed;
        this.capacity = capacity;
        this.status = status;
    }

    public Status status() { return status; }
    public void setStatus(Status status) { this.status = status; }
    protected abstract RoomPackage_2703142 roomPackage();
    public String type() { return roomPackage().label; }
    public int dailyRate() { return roomPackage().dailyRate; }
    public int extraGuestRate() { return roomPackage().extraGuestRate; }
    public int taxPercent() { return roomPackage().taxPercent; }
    public boolean includesBreakfast() { return roomPackage().breakfast; }

    public static Room_2703142 create(String type, String id, Bed bed, int capacity, Status status) {
        switch (type) {
            case "Standard": return new StandardRoom_2703142(id, bed, capacity, status);
            case "Deluxe": return new DeluxeRoom_2703142(id, bed, capacity, status);
            case "Suite": return new SuiteRoom_2703142(id, bed, capacity, status);
            case "Family Suite": return new FamilySuiteRoom_2703142(id, bed, capacity, status);
            default: throw new IllegalArgumentException("Unknown room type.");
        }
    }
}
