package velora.service;

import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.*;

import java.util.*;

public class HotelEngine {
    private final String hotelName;
    private final List<Room> inventory = new ArrayList<>();
    private final Map<String, Booking> activeBookings = new HashMap<>();

    public HotelEngine(String hotelName) {
        this.hotelName = hotelName;
    }

    public void registerRoom(Room room) { inventory.add(room); }

    public List<Room> getAvailableRooms() {
        List<Room> vacant = new ArrayList<>();
        for (Room r : inventory) {
            if (!r.isBooked()) vacant.add(r);
        }
        return vacant;
    }

    public Booking processReservation(Guest guest, int roomNumber, int nights)
            throws RoomNotAvailableException, InvalidBookingException {
        if (nights <= 0) throw new InvalidBookingException("Duration must be at least 1 night.");

        Room target = inventory.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .findFirst()
                .orElseThrow(() -> new RoomNotAvailableException("Suite #" + roomNumber + " does not exist."));

        if (target.isBooked()) {
            throw new RoomNotAvailableException("Suite #" + roomNumber + " is currently occupied.");
        }

        String bookingId = "BK-" + (1000 + new Random().nextInt(9000));
        Booking booking = new Booking(bookingId, guest, target, nights);
        activeBookings.put(bookingId, booking);
        return booking;
    }

    public Booking lookupBooking(String bookingId) {
        return activeBookings.get(bookingId);
    }

    public void finalizeCheckout(String bookingId) {
        Booking b = activeBookings.remove(bookingId);
        if (b != null) {
            b.getRoom().vacate();
        }
    }

    public String getHotelName() { return hotelName; }
}