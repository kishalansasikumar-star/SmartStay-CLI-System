package smartstay.model;

public abstract class E245549_Room {
    public enum Bed { Single, Double, Queen, King }
    public enum Status { Available, Reserved, UnderMaintenance }

    public final String id;
    public final Bed bed;
    public final int capacity;
    private Status status;

    protected E245549_Room(String id, Bed bed, int capacity, Status status) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive.");
        if (!id.matches("[A-Za-z0-9-]+")) throw new IllegalArgumentException("Use letters, digits or hyphens for the room ID.");
        this.id = id.toUpperCase(java.util.Locale.ROOT);
        this.bed = bed;
        this.capacity = capacity;
        this.status = status;
    }

    public Status status() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public abstract String type();
    public abstract int dailyRate();
    public abstract int extraGuestRate();
    public abstract int taxPercent();
    public abstract boolean includesBreakfast();

    public static E245549_Room create(String type, String id, Bed bed, int capacity, Status status) {
        switch (type) {
            case "Standard": return new E245549_StandardRoom(id, bed, capacity, status);
            case "Deluxe": return new E245549_DeluxeRoom(id, bed, capacity, status);
            case "Suite": return new E245549_SuiteRoom(id, bed, capacity, status);
            case "Family Suite": return new E245549_FamilySuiteRoom(id, bed, capacity, status);
            default: throw new IllegalArgumentException("Unknown room type.");
        }
    }
}
