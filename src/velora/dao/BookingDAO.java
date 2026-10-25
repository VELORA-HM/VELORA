package velora.dao;

import velora.model.Booking;
import velora.service.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    /** One row of the v_booking_details view, for the ledger screen. */
    public record BookingSummary(String bookingRef, String guestName, String phone, int roomNumber,
                                 String suiteName, String tier, String checkIn, String checkOut, int nights,
                                 double totalAmount, String paymentMethod, String paymentReference,
                                 String status, String createdAt) {}

    public boolean refExists(Connection conn, String ref) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM bookings WHERE booking_ref = ?")) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection conn, Booking b) throws SQLException {
        String sql = "INSERT INTO bookings (booking_ref, guest_id, room_number, check_in, check_out, total_amount) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getBookingRef());
            ps.setInt(2, b.getGuest().getGuestId());
            ps.setInt(3, b.getRoom().getRoomNumber());
            ps.setDate(4, Date.valueOf(b.getCheckIn()));
            ps.setDate(5, Date.valueOf(b.getCheckOut()));
            ps.setDouble(6, b.getTotalTariff());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                b.setBookingId(keys.getInt(1));
            }
        }
    }

    public List<BookingSummary> findAllDetails() throws SQLException {
        String sql = "SELECT * FROM v_booking_details ORDER BY created_at DESC";
        List<BookingSummary> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BookingSummary(
                        rs.getString("booking_ref"), rs.getString("guest_name"), rs.getString("phone"),
                        rs.getInt("room_number"), rs.getString("suite_name"), rs.getString("tier"),
                        rs.getString("check_in"), rs.getString("check_out"), rs.getInt("nights"),
                        rs.getDouble("total_amount"), rs.getString("payment_method"),
                        rs.getString("payment_reference"), rs.getString("status"),
                        rs.getString("created_at")));
            }
        }
        return list;
    }

    /** Checkout runs inside MySQL as the stored procedure sp_checkout (a CallableStatement). */
    public void checkout(String bookingRef) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_checkout(?)}")) {
            cs.setString(1, bookingRef);
            cs.execute();
        }
    }
}
