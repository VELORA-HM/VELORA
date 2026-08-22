package velora;

import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.*;
import velora.payment.*;
import velora.service.HotelEngine;
import velora.service.InvoiceService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        HotelEngine velora = new HotelEngine("VELORA GRAND RESORT");

        // Seed inventory with Standard, Deluxe, and Penthouse tiers
        velora.registerRoom(new StandardRoom(101, 120.0));
        velora.registerRoom(new StandardRoom(102, 120.0));
        velora.registerRoom(new DeluxeRoom(201, 250.0));
        velora.registerRoom(new DeluxeRoom(202, 300.0));
        velora.registerRoom(new PenthouseSuite(301, 750.0));

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║            V E L O R A   G R A N D                   ║");
        System.out.println("║       LUXURY HOSPITALITY MANAGEMENT PROTOCOL         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        while (running) {
            System.out.println("\n[1] View Available Suites");
            System.out.println("[2] Reserve a Suite (Check-In)");
            System.out.println("[3] Charge Penthouse Mini-Bar / Add-ons");
            System.out.println("[4] Process Checkout & Settle Bill");
            System.out.println("[5] Terminate System");
            System.out.print(">> Select Operation: ");

            if (!sc.hasNextInt()) {
                System.out.println("✖ Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("\n--- CURRENT VACANT INVENTORY ---");
                    velora.getAvailableRooms().forEach(System.out::println);
                    break;

                case 2:
                    System.out.print("\nGuest Full Name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Contact Number : ");
                    String phone = sc.nextLine().trim();
                    Guest guest = new Guest("GST-" + (100 + (int)(Math.random() * 900)), name, phone);

                    System.out.print("Enter Target Suite Number: ");
                    int roomNum = sc.nextInt();
                    System.out.print("Duration of Stay (Nights): ");
                    int nights = sc.nextInt();
                    sc.nextLine();

                    try {
                        Booking b = velora.processReservation(guest, roomNum, nights);
                        System.out.println("\n✔ RESERVATION SUCCESSFUL");
                        System.out.println("  Booking Ref : " + b.getBookingId());
                        System.out.println("  Guest Name  : " + b.getGuest().getFullName());
                        System.out.println("  Initial Due : $" + String.format("%.2f", b.getTotalTariff()));
                    } catch (RoomNotAvailableException | InvalidBookingException e) {
                        System.out.println("✖ ERROR: " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.print("\nEnter Booking Reference ID: ");
                    String barRef = sc.nextLine().trim();
                    Booking barBooking = velora.lookupBooking(barRef);

                    if (barBooking == null) {
                        System.out.println("✖ Booking reference not found.");
                        break;
                    }

                    if (barBooking.getRoom() instanceof PenthouseSuite) {
                        PenthouseSuite ps = (PenthouseSuite) barBooking.getRoom();
                        System.out.print("Enter Mini-Bar / Lounge charge to add: $");
                        double barCost = sc.nextDouble();
                        sc.nextLine();
                        ps.addMiniBarUsage(barCost);
                        System.out.printf("✔ Added $%.2f. Updated Total: $%.2f\n", barCost, ps.calculateTariff(barBooking.getNights()));
                    } else {
                        System.out.println("✖ Mini-Bar billing is exclusive to Penthouse suites.");
                    }
                    break;

                case 4:
                    System.out.print("\nEnter Booking Reference ID: ");
                    String ref = sc.nextLine().trim();
                    Booking b = velora.lookupBooking(ref);
                    if (b == null) {
                        System.out.println("✖ Booking reference not found.");
                        break;
                    }

                    double totalDue = b.getRoom().calculateTariff(b.getNights());
                    System.out.printf("\nBill Balance for %s: $%.2f\n", b.getGuest().getFullName(), totalDue);
                    System.out.println("Payment Gateway: [1] Credit Card  [2] Cash");
                    System.out.print("Selection: ");
                    int pType = sc.nextInt();
                    sc.nextLine();

                    Payment pEngine;
                    if (pType == 1) {
                        System.out.print("Enter 16-Digit Card Number: ");
                        String card = sc.nextLine().trim();
                        pEngine = new CardPayment("TXN-" + System.currentTimeMillis() % 100000, totalDue, card);
                    } else {
                        System.out.print("Enter Cash Amount Handed: ");
                        double cash = sc.nextDouble();
                        sc.nextLine();
                        pEngine = new CashPayment("TXN-" + System.currentTimeMillis() % 100000, totalDue, cash);
                    }

                    if (pEngine.processPayment(totalDue)) {
                        System.out.println("\n" + pEngine.generateReceipt());

                        String invoicePath = InvoiceService.generateInvoice(b, pEngine);
                        if (invoicePath != null) {
                            System.out.println("📄 Official Folio Saved: " + invoicePath);
                        }

                        velora.finalizeCheckout(ref);
                        System.out.println("✔ Suite #" + b.getRoom().getRoomNumber() + " is now vacated and cleaned.");
                    } else {
                        System.out.println("✖ Insufficient funds provided. Settlement failed.");
                    }
                    break;

                case 5:
                    running = false;
                    System.out.println("\nSystem safely disconnected.");
                    break;

                default:
                    System.out.println("✖ Invalid selection. Choose options 1 to 5.");
                    break;
            }
        }
        sc.close();
    }
}