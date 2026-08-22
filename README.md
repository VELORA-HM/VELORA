#  VELORA GRAND — Luxury Hospitality Management Protocol

An enterprise-grade, object-oriented Hotel Management Engine built with Core Java, File I/O persistence, MySQL JDBC integration, and modular domain architecture.

---

##  Key Architecture & Features

* **Object-Oriented Domain Layer:**
    * Strict abstraction and polymorphic behavior across `Room` models (`StandardRoom`, `DeluxeRoom`, `PenthouseSuite`).
    * Custom dynamic billing: Luxury Penthouse tax calculation (18%), concierge surcharge, and active mini-bar tab management.
* **Polymorphic Payment Pipeline:**
    * Decoupled `Payment` abstraction supporting multi-channel settlement (`CardPayment` with masking and `CashPayment` with tender calculations).
* **Automated Folio Generation (File I/O):**
    * Auto-generates structured, timestamped digital receipts (`.txt`) under the `/receipts` directory upon guest checkout.
* **Search & Multi-Criteria Filtering:**
    * Real-time querying by suite tier (`Standard`, `Deluxe`, `Penthouse`) or maximum budget threshold using Java Streams.
* **Resilient Data Layer & Logging:**
    * Pre-configured MySQL JDBC persistence with automatic fallback to in-memory session mode when offline.
    * Dedicated error tracking writing structured incident logs to `/logs/velora_errors.log`.

---

##  Package & Class Structure

```text
velora/
├── exception/
│   ├── InvalidBookingException.java
│   └── RoomNotAvailableException.java
├── model/
│   ├── Booking.java
│   ├── DeluxeRoom.java
│   ├── Guest.java
│   ├── PenthouseSuite.java
│   ├── Room.java
│   └── StandardRoom.java
├── payment/
│   ├── CardPayment.java
│   ├── CashPayment.java
│   ├── Payable.java
│   └── Payment.java
├── service/
│   ├── DBConnection.java
│   ├── HotelEngine.java
│   ├── InvoiceService.java
│   └── LoggerService.java
└── Main.java