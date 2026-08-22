package velora.model;

public class Booking {
    private final String bookingId;
    private final Guest guest;
    private final Room room;
    private final int nights;
    private final double totalTariff;

    public Booking(String bookingId, Guest guest, Room room, int nights) {
        this.bookingId = bookingId;
        this.guest = guest;
        this.room = room;
        this.nights = nights;
        this.totalTariff = room.calculateTariff(nights);
        this.room.book();
    }

    public String getBookingId() { return bookingId; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public int getNights() { return nights; }
    public double getTotalTariff() { return totalTariff; }
}