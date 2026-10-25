package velora.dao;

import velora.service.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public record TierRevenue(String tier, int bookings, double revenue) {}

    public List<TierRevenue> revenueByTier() throws SQLException {
        List<TierRevenue> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM v_revenue_by_type ORDER BY revenue DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TierRevenue(rs.getString("tier"), rs.getInt("total_bookings"), rs.getDouble("revenue")));
            }
        }
        return list;
    }
}
