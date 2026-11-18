package smartstay.model;

public class E245549_StandardRoom extends E245549_Room {
    public E245549_StandardRoom(String id, Bed bed, int capacity, Status status) {
        super(id, bed, capacity, status);
    }

    @Override protected E245549_RoomPackage roomPackage() {
        return E245549_RoomPackage.STANDARD;
    }
}
