package smartstay.model;

public enum RoomPackage {
    STANDARD("Standard", 6000, false, 1000, 10),
    DELUXE("Deluxe", 9000, true, 1500, 12),
    SUITE("Suite", 14000, true, 2000, 15),
    FAMILY_SUITE("Family Suite", 18000, true, 2500, 15);

    public final String label;
    public final int dailyRate, extraGuestRate, taxPercent;
    public final boolean breakfast;

    RoomPackage(String label, int dailyRate, boolean breakfast, int extraGuestRate, int taxPercent) {
        this.label = label;
        this.dailyRate = dailyRate;
        this.breakfast = breakfast;
        this.extraGuestRate = extraGuestRate;
        this.taxPercent = taxPercent;
    }
}
