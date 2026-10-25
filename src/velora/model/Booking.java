package velora.model;

import java.time.LocalDate;

public class Booking {
    private int bookingId;        // 0 until saved; then bookings.booking_id
    private final String bookingRef;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double totalTariff;

    public Booking(String bookingRef, Guest guest, Room room, LocalDate checkIn, int nights) {
        this.bookingRef = bookingRef;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkIn.plusDays(nights);
        this.totalTariff = room.calculateTariff(nights);   // polymorphic call
        this.room.book();
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getBookingRef() { return bookingRef; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return (int) (checkOut.toEpochDay() - checkIn.toEpochDay()); }
    public double getTotalTariff() { return totalTariff; }
}
