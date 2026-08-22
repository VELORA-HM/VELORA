import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

public class VeloraServer {

    private static final int PORT = 8080;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/velora_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root"; // <-- Change to your MySQL password if different

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/suites", new SuitesHandler());
        server.createContext("/api/reserve", new ReserveHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("=================================================");
        System.out.println(" VELORA GRAND PROTOCOL SERVER RUNNING ON PORT " + PORT);
        System.out.println(" REST API Endpoint: http://localhost:" + PORT + "/api/suites");
        System.out.println("=================================================");
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    static class SuitesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder json = new StringBuilder("[");
            String query = "SELECT * FROM rooms";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    int roomNum = rs.getInt("room_number");
                    String tier = rs.getString("tier");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price_per_night");
                    boolean isBooked = rs.getBoolean("is_booked");

                    String image = tier.equalsIgnoreCase("Penthouse")
                            ? "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80"
                            : (tier.equalsIgnoreCase("Deluxe")
                            ? "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80"
                            : "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80");

                    String desc = tier.equalsIgnoreCase("Penthouse")
                            ? "The pinnacle of bespoke luxury. Private rooftop infinity jacuzzi and dedicated butler."
                            : (tier.equalsIgnoreCase("Deluxe")
                            ? "Floor-to-ceiling panoramic glass offering skyline vistas and marble bath."
                            : "A minimalist sanctuary featuring acoustic soundproofing and high-speed fiber Wi-Fi.");

                    json.append(String.format(
                            "{\"roomNumber\":%d,\"tier\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"isBooked\":%b,\"image\":\"%s\",\"description\":\"%s\",\"amenities\":[\"High-Speed Wi-Fi\",\"4K Smart Display\",\"En-Suite Bar\"]}",
                            roomNum, tier, name, price, isBooked, image, desc
                    ));
                    first = false;
                }
            } catch (SQLException e) {
                System.err.println("[DB ERROR] " + e.getMessage());
            }

            json.append("]");

            byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    static class ReserveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String guestName = extractJsonValue(body, "name");
                String guestPhone = extractJsonValue(body, "phone");
                int roomNumber = Integer.parseInt(extractJsonValue(body, "roomNumber"));
                int nights = Integer.parseInt(extractJsonValue(body, "nights"));
                String tier = extractJsonValue(body, "tier");
                double basePrice = Double.parseDouble(extractJsonValue(body, "price"));

                double subtotal = basePrice * nights;
                double luxuryTax = tier.equalsIgnoreCase("Penthouse") ? subtotal * 0.18 : 0.0;
                double total = subtotal + luxuryTax;
                String bookingRef = "VEL-" + (10000 + (int)(Math.random() * 90000));

                // 1. Save to MySQL
                String insertBookingSql = "INSERT INTO bookings (booking_id, guest_name, guest_phone, room_number, nights, total_tariff) VALUES (?, ?, ?, ?, ?, ?)";
                String updateRoomSql = "UPDATE rooms SET is_booked = TRUE WHERE room_number = ?";

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    conn.setAutoCommit(false);

                    try (PreparedStatement bStmt = conn.prepareStatement(insertBookingSql);
                         PreparedStatement rStmt = conn.prepareStatement(updateRoomSql)) {

                        bStmt.setString(1, bookingRef);
                        bStmt.setString(2, guestName);
                        bStmt.setString(3, guestPhone);
                        bStmt.setInt(4, roomNumber);
                        bStmt.setInt(5, nights);
                        bStmt.setDouble(6, total);
                        bStmt.executeUpdate();

                        rStmt.setInt(1, roomNumber);
                        rStmt.executeUpdate();

                        conn.commit();
                        System.out.println("[SQL TRANSACTION COMMITTED] Booking " + bookingRef + " saved to MySQL.");
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    }
                } catch (SQLException e) {
                    System.err.println("[DATABASE WRITE ERROR] " + e.getMessage());
                }

                // 2. Generate Digital File Invoice
                generateFolioInvoice(bookingRef, guestName, guestPhone, roomNumber, tier, nights, total);

                String responseJson = String.format(
                        "{\"status\":\"SUCCESS\",\"bookingId\":\"%s\",\"total\":%.2f,\"message\":\"Reservation confirmed and saved to MySQL\"}",
                        bookingRef, total
                );

                byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) return m.group(1).trim().replace("\"", "");
            return "";
        }

        private void generateFolioInvoice(String ref, String guest, String phone, int room, String tier, int nights, double total) {
            try {
                File dir = new File("logs/invoices");
                if (!dir.exists()) dir.mkdirs();

                File invoiceFile = new File(dir, "Invoice_" + ref + ".txt");
                try (FileWriter writer = new FileWriter(invoiceFile)) {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    writer.write("====================================================\n");
                    writer.write("               VELORA GRAND RESORT                  \n");
                    writer.write("          OFFICIAL DIGITAL GUEST FOLIO              \n");
                    writer.write("====================================================\n");
                    writer.write("Folio Reference : " + ref + "\n");
                    writer.write("Timestamp       : " + timestamp + "\n");
                    writer.write("Guest Name      : " + guest + "\n");
                    writer.write("Contact Number  : " + phone + "\n");
                    writer.write("----------------------------------------------------\n");
                    writer.write("Suite Reserved  : #" + room + " (" + tier + " Tier)\n");
                    writer.write("Stay Duration   : " + nights + " Night(s)\n");
                    writer.write("Settled Tariff  : $" + String.format("%.2f", total) + "\n");
                    writer.write("====================================================\n");
                }
            } catch (Exception e) {
                System.err.println("[FILE ERROR] " + e.getMessage());
            }
        }
    }
}