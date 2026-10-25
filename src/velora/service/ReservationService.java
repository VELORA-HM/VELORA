package velora.service;

import velora.dao.BookingDAO;
import velora.dao.GuestDAO;
import velora.dao.PaymentDAO;
import velora.dao.RoomDAO;
import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.Booking;
import velora.model.Guest;
import velora.model.Room;
import velora.payment.CardPayment;
import velora.payment.CashPayment;
import velora.payment.Payment;
import velora.payment.UpiPayment;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

/**
 * The bridge between the OOP model and MySQL.
 * One reservation = one transaction across four tables: guests, bookings, payments, rooms.
 */
public class ReservationService {
    private final RoomDAO roomDAO = new RoomDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final Random random = new Random();

    public Booking reserve(Guest guest, int roomNumber, LocalDate checkIn, int nights,
                           String paymentMethod, String paymentDetail)
            throws InvalidBookingException, RoomNotAvailableException, SQLException {

        if (nights < 1 || nights > 30) throw new InvalidBookingException("Stay must be between 1 and 30 nights.");
        if (checkIn.isBefore(LocalDate.now())) throw new InvalidBookingException("Check-in date cannot be in the past.");
        if (guest.getFullName().isBlank() || !guest.getPhone().matches("\\d{10}"))
            throw new InvalidBookingException("Enter a name and a 10-digit phone number.");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);                                   // BEGIN TRANSACTION
            try {
                Room room = roomDAO.lockForBooking(conn, roomNumber);    // SELECT ... FOR UPDATE
                if (room == null) throw new RoomNotAvailableException("Suite #" + roomNumber + " does not exist.");
                if (room.isBooked()) throw new RoomNotAvailableException("Suite #" + roomNumber + " is already occupied.");

                guestDAO.saveOrUpdate(conn, guest);                      // guests

                Booking booking = new Booking(newRef(conn), guest, room, checkIn, nights);  // calculateTariff()
                bookingDAO.insert(conn, booking);                        // bookings (+ audit trigger)

                if (paymentMethod != null) {                             // console flow pays at checkout instead
                    Payment payment = createPayment(paymentMethod, paymentDetail, booking.getTotalTariff());
                    if (!payment.processPayment(booking.getTotalTariff()))
                        throw new InvalidBookingException("Payment could not be processed. Check the payment details.");
                    paymentDAO.insert(conn, booking.getBookingId(), payment); // payments
                }

                roomDAO.updateStatus(conn, roomNumber, "OCCUPIED");      // rooms
                conn.commit();                                           // all four writes together
                return booking;
            } catch (Exception e) {
                conn.rollback();                                         // or none of them
                throw e;
            }
        }
    }

    /** Polymorphism again: the right Payment subclass for the method the guest picked. */
    private Payment createPayment(String method, String detail, double amount) throws InvalidBookingException {
        String txn = "TXN-" + System.currentTimeMillis() % 1_000_000;
        switch (method == null ? "" : method.toUpperCase()) {
            case "CARD":
                if (detail == null || !detail.matches("\\d{4}"))
                    throw new InvalidBookingException("Enter the last 4 digits of the card.");
                return new CardPayment(txn, amount, detail);
            case "UPI":
                return new UpiPayment(txn, amount, detail);
            case "CASH":
                return new CashPayment(txn, amount, amount);
            default:
                throw new InvalidBookingException("Choose Card, UPI or Cash.");
        }
    }

    private String newRef(Connection conn) throws SQLException {
        String ref;
        do {
            ref = "VEL-" + (10000 + random.nextInt(90000));
        } while (bookingDAO.refExists(conn, ref));
        return ref;
    }
}
