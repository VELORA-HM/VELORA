package velora.service;

import velora.dao.BookingDAO;
import velora.dao.PaymentDAO;
import velora.dao.RoomDAO;
import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.*;
import velora.payment.Payment;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Console front desk (Main.java). Uses the same DAO + service layer as the web server. */
public class HotelEngine {
    private final String hotelName;
    private final List<Room> inventory = new ArrayList<>();
    private final Map<String, Booking> activeBookings = new HashMap<>();
    private final ReservationService reservations = new ReservationService();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public HotelEngine(String hotelName) {
        this.hotelName = hotelName;
        loadRoomsFromDatabase();
    }

    private void loadRoomsFromDatabase() {
        try {
            inventory.clear();
            inventory.addAll(new RoomDAO().findAll());
        } catch (SQLException e) {
            LoggerService.logError("Database Room Sync", e);
            System.out.println("⚠ Notice: MySQL database offline. Operating in in-memory session mode.");
        }
    }

    public void registerRoom(Room room) {
        inventory.add(room);
    }

    public List<Room> getAvailableRooms() {
        return inventory.stream().filter(r -> !r.isBooked()).collect(Collectors.toList());
    }

    public List<Room> filterRoomsByType(String type) {
        return inventory.stream()
                .filter(r -> r.getTier().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public List<Room> filterRoomsByMaxPrice(double maxPrice) {
        return inventory.stream()
                .filter(r -> r.getBasePrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    public Booking processReservation(Guest guest, int roomNumber, int nights)
            throws RoomNotAvailableException, InvalidBookingException, SQLException {
        Booking booking = reservations.reserve(guest, roomNumber, LocalDate.now(), nights, null, null);
        inventory.stream().filter(r -> r.getRoomNumber() == roomNumber).findFirst().ifPresent(Room::book);
        activeBookings.put(booking.getBookingRef(), booking);
        return booking;
    }

    public Booking lookupBooking(String bookingRef) {
        return activeBookings.get(bookingRef);
    }

    public void finalizeCheckout(String bookingRef, Payment payment) {
        Booking b = activeBookings.remove(bookingRef);
        if (b != null) {
            b.getRoom().vacate();
            inventory.stream().filter(r -> r.getRoomNumber() == b.getRoom().getRoomNumber())
                    .findFirst().ifPresent(Room::vacate);
            try (Connection conn = DBConnection.getConnection()) {
                paymentDAO.insert(conn, b.getBookingId(), payment);
                bookingDAO.checkout(bookingRef);
            } catch (SQLException e) {
                LoggerService.logError("Checkout SQL Sync", e);
            }
        }
    }

    public String getHotelName() { return hotelName; }
}
