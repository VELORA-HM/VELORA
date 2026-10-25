package velora.dao;

import velora.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** A returning guest (same phone) is reused instead of duplicated. */
public class GuestDAO {

    public void saveOrUpdate(Connection conn, Guest guest) throws SQLException {
        try (PreparedStatement find = conn.prepareStatement("SELECT guest_id FROM guests WHERE phone = ?")) {
            find.setString(1, guest.getPhone());
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    guest.setGuestId(rs.getInt(1));
                    try (PreparedStatement up = conn.prepareStatement(
                            "UPDATE guests SET full_name = ?, email = COALESCE(?, email) WHERE guest_id = ?")) {
                        up.setString(1, guest.getFullName());
                        up.setString(2, guest.getEmail());
                        up.setInt(3, guest.getGuestId());
                        up.executeUpdate();
                    }
                    return;
                }
            }
        }

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO guests (full_name, phone, email) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, guest.getFullName());
            ins.setString(2, guest.getPhone());
            ins.setString(3, guest.getEmail());
            ins.executeUpdate();
            try (ResultSet keys = ins.getGeneratedKeys()) {
                keys.next();
                guest.setGuestId(keys.getInt(1));
            }
        }
    }
}
