package velora.model;

/** Turns a row of rooms + room_types into the right Room subclass. */
public class RoomFactory {
    public static Room create(String tier, int roomNumber, String name, double price) {
        switch (tier) {
            case "Deluxe":    return new DeluxeRoom(roomNumber, name, price);
            case "Penthouse": return new PenthouseSuite(roomNumber, name, price);
            default:          return new StandardRoom(roomNumber, name, price);
        }
    }
}
