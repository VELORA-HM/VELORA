import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import velora.dao.BookingDAO;
import velora.dao.ReportDAO;
import velora.dao.RoomDAO;
import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.Booking;
import velora.model.Guest;
import velora.model.Room;
import velora.service.DBConnection;
import velora.service.LoggerService;
import velora.service.ReservationService;
import velora.web.Json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;

/**
 * REST API for the React frontend. Every endpoint goes through the OOP layer:
 * handlers -> ReservationService / DAOs -> DBConnection -> MySQL.
 */
public class VeloraServer {

    private static final int PORT = 8080;
    private static final RoomDAO roomDAO = new RoomDAO();
    private static final BookingDAO bookingDAO = new BookingDAO();
    private static final ReportDAO reportDAO = new ReportDAO();
    private static final ReservationService reservationService = new ReservationService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/suites", api(VeloraServer::suites));
        server.createContext("/api/quote", api(VeloraServer::quote));
        server.createContext("/api/reserve", api(VeloraServer::reserve));
        server.createContext("/api/bookings", api(VeloraServer::bookings));
        server.createContext("/api/checkout", api(VeloraServer::checkout));
        server.createContext("/api/stats", api(VeloraServer::stats));

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("=================================================");
        System.out.println(" VELORA GRAND PROTOCOL SERVER RUNNING ON PORT " + PORT);
        System.out.println(" Database : " + DBConnection.describe());
        System.out.println(" Try      : http://localhost:" + PORT + "/api/suites");
        System.out.println("=================================================");
    }

    // ------------------------------------------------------------------ endpoints

    /** GET /api/suites — every suite with tier, status and amenities from MySQL. */
    private static Response suites(HttpExchange ex) throws Exception {
        StringJoiner arr = new StringJoiner(",", "[", "]");
        for (Room r : roomDAO.findAll()) {
            StringJoiner amen = new StringJoiner(",", "[", "]");
            r.getAmenities().forEach(a -> amen.add(Json.str(a)));
            arr.add("{\"roomNumber\":" + r.getRoomNumber()
                    + ",\"tier\":" + Json.str(r.getTier())
                    + ",\"name\":" + Json.str(r.getName())
                    + ",\"price\":" + Json.num(r.getBasePrice())
                    + ",\"isBooked\":" + r.isBooked()
                    + ",\"image\":" + Json.str(r.getImageUrl())
                    + ",\"description\":" + Json.str(r.getDescription())
                    + ",\"tariffRule\":" + Json.str(r.getTariffRule())
                    + ",\"amenities\":" + amen + "}");
        }
        return Response.ok(arr.toString());
    }

    /** GET /api/quote?room=201&nights=2 — the bill as the Room subclass calculates it. */
    private static Response quote(HttpExchange ex) throws Exception {
        Map<String, String> q = query(ex);
        Room room = roomDAO.findByNumber(Integer.parseInt(q.getOrDefault("room", "0")));
        if (room == null) return Response.error(404, "No such suite.");
        int nights = Integer.parseInt(q.getOrDefault("nights", "1"));
        if (nights < 1 || nights > 30) return Response.error(400, "Stay must be between 1 and 30 nights.");
        double total = room.calculateTariff(nights);
        return Response.ok("{\"roomNumber\":" + room.getRoomNumber()
                + ",\"javaClass\":" + Json.str(room.getClass().getSimpleName())
                + ",\"rule\":" + Json.str(room.getTariffRule())
                + ",\"subtotal\":" + Json.num(room.getBasePrice() * nights)
                + ",\"total\":" + Json.num(total) + "}");
    }

    /** POST /api/reserve — guest + booking + payment + room status in one transaction. */
    private static Response reserve(HttpExchange ex) throws Exception {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) return Response.error(405, "Use POST.");
        Map<String, String> body = Json.parseObject(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        Guest guest = new Guest(body.getOrDefault("name", "").trim(),
                body.getOrDefault("phone", "").trim(), body.get("email"));
        LocalDate checkIn;
        try {
            checkIn = LocalDate.parse(body.getOrDefault("checkIn", LocalDate.now().toString()));
        } catch (DateTimeParseException e) {
            return Response.error(400, "Check-in date must look like 2026-10-25.");
        }

        try {
            Booking b = reservationService.reserve(guest,
                    Integer.parseInt(body.getOrDefault("roomNumber", "0")),
                    checkIn,
                    Integer.parseInt(body.getOrDefault("nights", "0")),
                    body.get("paymentMethod"),
                    body.get("paymentDetail"));
            writeFolio(b, body.get("paymentMethod"));
            System.out.println("[SQL TRANSACTION COMMITTED] " + b.getBookingRef() + " -> suite #" + b.getRoom().getRoomNumber());
            return Response.ok("{\"status\":\"SUCCESS\",\"bookingId\":" + Json.str(b.getBookingRef())
                    + ",\"guestId\":" + b.getGuest().getGuestId()
                    + ",\"checkIn\":" + Json.str(b.getCheckIn().toString())
                    + ",\"checkOut\":" + Json.str(b.getCheckOut().toString())
                    + ",\"total\":" + Json.num(b.getTotalTariff())
                    + ",\"message\":\"Reservation saved to MySQL\"}");
        } catch (InvalidBookingException e) {
            return Response.error(400, e.getMessage());
        } catch (RoomNotAvailableException e) {
            return Response.error(409, e.getMessage());
        }
    }

    /** GET /api/bookings — the reservations ledger, read from the v_booking_details view. */
    private static Response bookings(HttpExchange ex) throws Exception {
        StringJoiner arr = new StringJoiner(",", "[", "]");
        for (BookingDAO.BookingSummary b : bookingDAO.findAllDetails()) {
            arr.add("{\"bookingRef\":" + Json.str(b.bookingRef())
                    + ",\"guestName\":" + Json.str(b.guestName())
                    + ",\"phone\":" + Json.str(b.phone())
                    + ",\"roomNumber\":" + b.roomNumber()
                    + ",\"suiteName\":" + Json.str(b.suiteName())
                    + ",\"tier\":" + Json.str(b.tier())
                    + ",\"checkIn\":" + Json.str(b.checkIn())
                    + ",\"checkOut\":" + Json.str(b.checkOut())
                    + ",\"nights\":" + b.nights()
                    + ",\"total\":" + Json.num(b.totalAmount())
                    + ",\"paymentMethod\":" + Json.str(b.paymentMethod())
                    + ",\"paymentReference\":" + Json.str(b.paymentReference())
                    + ",\"status\":" + Json.str(b.status())
                    + ",\"createdAt\":" + Json.str(b.createdAt()) + "}");
        }
        return Response.ok(arr.toString());
    }

    /** POST /api/checkout {"bookingRef":"VEL-12345"} — runs the stored procedure sp_checkout. */
    private static Response checkout(HttpExchange ex) throws Exception {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) return Response.error(405, "Use POST.");
        Map<String, String> body = Json.parseObject(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String ref = body.getOrDefault("bookingRef", "");
        try {
            bookingDAO.checkout(ref);
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) return Response.error(404, e.getMessage());
            throw e;
        }
        return Response.ok("{\"status\":\"SUCCESS\",\"bookingRef\":" + Json.str(ref) + "}");
    }

    /** GET /api/stats — revenue per tier from the v_revenue_by_type view. */
    private static Response stats(HttpExchange ex) throws Exception {
        StringJoiner arr = new StringJoiner(",", "[", "]");
        for (ReportDAO.TierRevenue t : reportDAO.revenueByTier()) {
            arr.add("{\"tier\":" + Json.str(t.tier()) + ",\"bookings\":" + t.bookings()
                    + ",\"revenue\":" + Json.num(t.revenue()) + "}");
        }
        return Response.ok(arr.toString());
    }

    // ------------------------------------------------------------------ plumbing

    private interface Endpoint { Response handle(HttpExchange ex) throws Exception; }

    private record Response(int code, String json) {
        static Response ok(String json) { return new Response(200, json); }
        static Response error(int code, String message) {
            return new Response(code, "{\"status\":\"ERROR\",\"message\":" + Json.str(message) + "}");
        }
    }

    /** Wraps an endpoint with CORS, JSON headers and error handling. */
    private static HttpHandler api(Endpoint endpoint) {
        return ex -> {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            Response res;
            try {
                res = endpoint.handle(ex);
            } catch (SQLException e) {
                LoggerService.logError("API " + ex.getRequestURI(), e);
                System.err.println("[DB ERROR] " + e.getMessage());
                res = Response.error(503, "Database error: " + e.getMessage());
            } catch (NumberFormatException e) {
                res = Response.error(400, "Room number and nights must be numbers.");
            } catch (Exception e) {
                LoggerService.logError("API " + ex.getRequestURI(), e);
                res = Response.error(500, e.getMessage());
            }
            byte[] bytes = res.json().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(res.code(), bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        };
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return map;
    }

    /** Text folio per booking in logs/invoices (file I/O alongside the database). */
    private static void writeFolio(Booking b, String method) {
        File dir = new File("logs/invoices");
        if (!dir.exists()) dir.mkdirs();
        try (FileWriter w = new FileWriter(new File(dir, "Invoice_" + b.getBookingRef() + ".txt"))) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            w.write("====================================================\n");
            w.write("               VELORA GRAND RESORT                  \n");
            w.write("          OFFICIAL DIGITAL GUEST FOLIO              \n");
            w.write("====================================================\n");
            w.write("Folio Reference : " + b.getBookingRef() + "\n");
            w.write("Timestamp       : " + ts + "\n");
            w.write("Guest Name      : " + b.getGuest().getFullName() + "\n");
            w.write("Contact Number  : " + b.getGuest().getPhone() + "\n");
            w.write("----------------------------------------------------\n");
            w.write("Suite Reserved  : #" + b.getRoom().getRoomNumber() + " (" + b.getRoom().getTier() + " Tier)\n");
            w.write("Stay            : " + b.getCheckIn() + " to " + b.getCheckOut() + " (" + b.getNights() + " night(s))\n");
            w.write("Payment         : " + method + "\n");
            w.write("Settled Tariff  : ₹" + String.format("%.2f", b.getTotalTariff()) + "\n");
            w.write("====================================================\n");
        } catch (IOException e) {
            LoggerService.logError("Folio write", e);
        }
    }
}
