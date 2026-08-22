package velora.service;

import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class HotelEngine {
    private final String hotelName;
    private final List<Room> inventory = new ArrayList<>();
    private final Map<String, Booking> activeBookings = new HashMap<>();

    public HotelEngine(String hotelName) {
        this.hotelName = hotelName;
        loadRoomsFromDatabase();
    }

    private void loadRoomsFromDatabase() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {

            inventory.clear();
            while (rs.next()) {
                int rNum = rs.getInt("room_number");
                String rType = rs.getString("room_type");
                double price = rs.getDouble("base_price");
                boolean isBooked = rs.getBoolean("is_booked");

                Room room;
                if ("DeluxeRoom".equalsIgnoreCase(rType)) {
                    room = new DeluxeRoom(rNum, price);
                } else if ("PenthouseSuite".equalsIgnoreCase(rType)) {
                    room = new PenthouseSuite(rNum, price);
                } else {
                    room = new StandardRoom(rNum, price);
                }

                if (isBooked) room.book();
                inventory.add(room);
            }
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
                .filter(r -> r.getClass().getSimpleName().toLowerCase().contains(type.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Room> filterRoomsByMaxPrice(double maxPrice) {
        return inventory.stream()
                .filter(r -> r.getBasePrice() <= maxPrice)
                .collect(Collectors.toList());
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

        try (Connection conn = DBConnection.getConnection()) {
            String insertGuest = "INSERT INTO guests (guest_id, full_name, phone) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE full_name=?, phone=?";
            try (PreparedStatement ps = conn.prepareStatement(insertGuest)) {
                ps.setString(1, guest.getGuestId());
                ps.setString(2, guest.getFullName());
                ps.setString(3, guest.getPhone());
                ps.setString(4, guest.getFullName());
                ps.setString(5, guest.getPhone());
                ps.executeUpdate();
            }

            String insertBooking = "INSERT INTO bookings (booking_id, guest_id, room_number, nights, total_tariff, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
            try (PreparedStatement ps = conn.prepareStatement(insertBooking)) {
                ps.setString(1, bookingId);
                ps.setString(2, guest.getGuestId());
                ps.setInt(3, roomNumber);
                ps.setInt(4, nights);
                ps.setDouble(5, booking.getTotalTariff());
                ps.executeUpdate();
            }

            String updateRoom = "UPDATE rooms SET is_booked = TRUE WHERE room_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateRoom)) {
                ps.setInt(1, roomNumber);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LoggerService.logError("Reservation SQL Sync", e);
        }

        return booking;
    }

    public Booking lookupBooking(String bookingId) {
        return activeBookings.get(bookingId);
    }

    public void finalizeCheckout(String bookingId) {
        Booking b = activeBookings.remove(bookingId);
        if (b != null) {
            b.getRoom().vacate();

            try (Connection conn = DBConnection.getConnection()) {
                String updateBooking = "UPDATE bookings SET status = 'SETTLED' WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                    ps.setString(1, bookingId);
                    ps.executeUpdate();
                }

                String updateRoom = "UPDATE rooms SET is_booked = FALSE WHERE room_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateRoom)) {
                    ps.setInt(1, b.getRoom().getRoomNumber());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                LoggerService.logError("Checkout SQL Sync", e);
            }
        }
    }

    public String getHotelName() { return hotelName; }
}