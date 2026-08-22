package velora;

import velora.exception.InvalidBookingException;
import velora.exception.RoomNotAvailableException;
import velora.model.*;
import velora.payment.*;
import velora.service.HotelEngine;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        HotelEngine velora = new HotelEngine("VELORA GRAND RESORT");

        // Seed inventory
        velora.registerRoom(new StandardRoom(101, 120.0));
        velora.registerRoom(new StandardRoom(102, 120.0));
        velora.registerRoom(new DeluxeRoom(201, 250.0));
        velora.registerRoom(new DeluxeRoom(202, 300.0));

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║            V E L O R A   G R A N D                   ║");
        System.out.println("║       LUXURY HOSPITALITY MANAGEMENT PROTOCOL         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        while (running) {
            System.out.println("\n[1] View Available Suites");
            System.out.println("[2] Reserve a Suite (Check-In)");
            System.out.println("[3] Process Checkout & Settle Bill");
            System.out.println("[4] Terminate System");
            System.out.print(">> Select Operation: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("\n--- CURRENT VACANT INVENTORY ---");
                    velora.getAvailableRooms().forEach(System.out::println);
                    break;

                case 2:
                    System.out.print("\nGuest Full Name: ");
                    String name = sc.nextLine();
                    System.out.print("Contact Number : ");
                    String phone = sc.nextLine();
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
                        System.out.println("  Total Due   : $" + String.format("%.2f", b.getTotalTariff()));
                    } catch (RoomNotAvailableException | InvalidBookingException e) {
                        System.out.println("✖ ERROR: " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.print("\nEnter Booking Reference ID: ");
                    String ref = sc.nextLine().trim();
                    Booking b = velora.lookupBooking(ref);
                    if (b == null) {
                        System.out.println("✖ Booking reference not found.");
                        break;
                    }

                    System.out.printf("Bill Balance for %s: $%.2f\n", b.getGuest().getFullName(), b.getTotalTariff());
                    System.out.println("Payment Gateway: [1] Credit Card  [2] Cash");
                    System.out.print("Selection: ");
                    int pType = sc.nextInt();
                    sc.nextLine();

                    Payment pEngine;
                    if (pType == 1) {
                        System.out.print("Enter 16-Digit Card Number: ");
                        String card = sc.nextLine();
                        pEngine = new CardPayment("TXN-" + System.currentTimeMillis() % 100000, b.getTotalTariff(), card);
                    } else {
                        System.out.print("Enter Cash Amount Handed: ");
                        double cash = sc.nextDouble();
                        sc.nextLine();
                        pEngine = new CashPayment("TXN-" + System.currentTimeMillis() % 100000, b.getTotalTariff(), cash);
                    }

                    if (pEngine.processPayment(b.getTotalTariff())) {
                        System.out.println("\n" + pEngine.generateReceipt());
                        velora.finalizeCheckout(ref);
                        System.out.println("✔ Suite #" + b.getRoom().getRoomNumber() + " is now vacated and cleaned.");
                    } else {
                        System.out.println("✖ Insufficient funds provided. Settlement failed.");
                    }
                    break;

                case 4:
                    running = false;
                    System.out.println("\nSystem safely disconnected.");
                    break;
            }
        }
        sc.close();
    }
}