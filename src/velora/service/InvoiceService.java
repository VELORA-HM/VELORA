package velora.service;

import velora.model.Booking;
import velora.payment.Payment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InvoiceService {
    private static final String RECEIPTS_DIR = "receipts";

    public static String generateInvoice(Booking booking, Payment payment) {
        // Ensure the directory exists
        File directory = new File(RECEIPTS_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = RECEIPTS_DIR + "/Invoice_" + booking.getBookingId() + ".txt";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = dtf.format(LocalDateTime.now());

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("==========================================================");
            writer.println("               V E L O R A   G R A N D                    ");
            writer.println("              OFFICIAL GUEST FOLIO & INVOICE              ");
            writer.println("==========================================================");
            writer.printf(" Generated On : %s\n", timestamp);
            writer.printf(" Booking Ref  : %s\n", booking.getBookingId());
            writer.println("----------------------------------------------------------");
            writer.println(" GUEST DETAILS:");
            writer.printf("  Guest ID    : %s\n", booking.getGuest().getGuestId());
            writer.printf("  Full Name   : %s\n", booking.getGuest().getFullName());
            writer.printf("  Contact     : %s\n", booking.getGuest().getPhone());
            writer.println("----------------------------------------------------------");
            writer.println(" STAY BREAKDOWN:");
            writer.printf("  Suite No.   : #%d\n", booking.getRoom().getRoomNumber());
            writer.printf("  Suite Type  : %s\n", booking.getRoom().getClass().getSimpleName());
            writer.printf("  Duration    : %d Night(s)\n", booking.getNights());
            writer.printf("  Total Due   : $%.2f\n", booking.getTotalTariff());
            writer.println("----------------------------------------------------------");
            writer.println(" PAYMENT SETTLEMENT:");
            writer.printf("  Method      : %s\n", payment.getPaymentMethod());
            writer.printf("  Status      : SETTLED & VERIFIED\n");
            writer.println("==========================================================");
            writer.println("       Thank you for staying at Velora Grand Resort.      ");
            writer.println("==========================================================");

            return fileName;
        } catch (IOException e) {
            System.err.println("Failed to write receipt file: " + e.getMessage());
            return null;
        }
    }
}