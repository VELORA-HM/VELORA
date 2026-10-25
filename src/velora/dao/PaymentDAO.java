package velora.dao;

import velora.payment.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentDAO {

    /** Works for any Payment subclass: method code and reference come from the object itself. */
    public void insert(Connection conn, int bookingId, Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, method, amount, reference) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setString(2, payment.getMethodCode());
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getReference());
            ps.executeUpdate();
        }
    }
}
