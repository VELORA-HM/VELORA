package velora.dao;

import velora.model.Room;
import velora.model.RoomFactory;
import velora.service.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rows of rooms + room_types + room_amenities  <->  Room objects. */
public class RoomDAO {

    private static final String SELECT_ROOMS =
            "SELECT r.room_number, r.name, r.description, r.image_url, r.price_per_night, r.status, t.type_name " +
            "FROM rooms r JOIN room_types t ON t.type_id = r.type_id ";

    public List<Room> findAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Map<Integer, List<String>> amenities = loadAmenities(conn);
            Map<Integer, Room> rooms = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(SELECT_ROOMS + "ORDER BY r.room_number");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Room room = toRoom(rs);
                    room.setDetails(rs.getString("description"), rs.getString("image_url"),
                            amenities.getOrDefault(room.getRoomNumber(), List.of()));
                    rooms.put(room.getRoomNumber(), room);
                }
            }
            return new ArrayList<>(rooms.values());
        }
    }

    public Room findByNumber(int roomNumber) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ROOMS + "WHERE r.room_number = ?")) {
            ps.setInt(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toRoom(rs) : null;
            }
        }
    }

    /** Locks the room row until the surrounding transaction commits (SELECT ... FOR UPDATE). */
    public Room lockForBooking(Connection conn, int roomNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ROOMS + "WHERE r.room_number = ? FOR UPDATE")) {
            ps.setInt(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toRoom(rs) : null;
            }
        }
    }

    public void updateStatus(Connection conn, int roomNumber, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE rooms SET status = ? WHERE room_number = ?")) {
            ps.setString(1, status);
            ps.setInt(2, roomNumber);
            ps.executeUpdate();
        }
    }

    private Room toRoom(ResultSet rs) throws SQLException {
        Room room = RoomFactory.create(rs.getString("type_name"), rs.getInt("room_number"),
                rs.getString("name"), rs.getDouble("price_per_night"));
        if (!"AVAILABLE".equals(rs.getString("status"))) room.book();
        return room;
    }

    private Map<Integer, List<String>> loadAmenities(Connection conn) throws SQLException {
        String sql = "SELECT ra.room_number, a.name FROM room_amenities ra " +
                     "JOIN amenities a ON a.amenity_id = ra.amenity_id ORDER BY ra.room_number, a.amenity_id";
        Map<Integer, List<String>> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getInt("room_number"), k -> new ArrayList<>()).add(rs.getString("name"));
            }
        }
        return map;
    }
}
