#  VELORA GRAND — Luxury Hospitality Management Protocol

A hotel suite reservation system: a React frontend, a Core Java OOP backend, and a normalized MySQL database (`velora_db`) connected through JDBC. Every reservation made on the website is saved to MySQL in a single transaction and shown back in a live Reservations Ledger.

---

##  Run it locally (demo checklist)

**1. Create the database.** Open `database/velora_schema.sql` in MySQL Workbench and run the whole file (⚡). It recreates `velora_db` with 8 tables, 3 views, a stored procedure, a function, 2 triggers and seed data.
> It drops and recreates `velora_db`, so any old test bookings are removed.

**2. Give Java your MySQL password.** Copy `db.properties.example` to `db.properties` and set `db.password`. (`db.properties` is git-ignored.)

**3. Start the backend.** Put the MySQL Connector/J jar (`mysql-connector-j-<version>.jar`, from dev.mysql.com/downloads/connector/j) in the `lib/` folder, then either:
- **VS Code** (with the Extension Pack for Java): open the project folder, go to *Run and Debug*, pick **VeloraServer (API on :8080)** and press ▶. `.vscode/settings.json` already points VS Code at `src/` and `lib/`.
- **Terminal:**
  ```bash
  ./run-server.sh
  ```

You should see `VELORA GRAND PROTOCOL SERVER RUNNING ON PORT 8080`. The console front desk is the **Main (console front desk)** run configuration.

**4. Start the frontend.**
```bash
cd velora-frontend && npm install && npm run dev
```
Open http://localhost:5173. The dot in the top bar turns green ("MySQL live") when the server is reachable.

**5. Demo.** Reserve a suite → the confirmation shows the booking reference → scroll to the Reservations Ledger → run `SELECT * FROM v_booking_details;` in Workbench to show the same row. Click **Check out** to run the stored procedure `sp_checkout`.

**EER diagram:** in Workbench, *Database → Reverse Engineer…* → pick `velora_db` → Finish.

---

##  Database (velora_db, 3NF)

| Table | Purpose | Keys |
| --- | --- | --- |
| `room_types` | Standard / Deluxe / Penthouse — decides the Java subclass | PK `type_id`, UNIQUE `type_name` |
| `rooms` | Each physical suite, its rate and status | PK `room_number`, FK `type_id` |
| `amenities` | Amenity names | PK `amenity_id`, UNIQUE `name` |
| `room_amenities` | Many-to-many rooms ↔ amenities | PK (`room_number`, `amenity_id`), 2 FKs |
| `guests` | Guest stored once, matched by phone | PK `guest_id`, UNIQUE `phone`, `email` |
| `bookings` | One row per reservation (nights derived from dates) | PK `booking_id`, UNIQUE `booking_ref`, FKs `guest_id`, `room_number` |
| `payments` | How a booking was paid (card last 4 / UPI ID only) | PK `payment_id`, FK `booking_id` |
| `booking_audit` | Status history, written only by triggers | PK `audit_id`, FK `booking_id` |

Views: `v_booking_details`, `v_revenue_by_type`, `v_available_rooms` · Procedure: `sp_checkout` · Function: `fn_stay_nights` · Triggers: `trg_booking_created`, `trg_booking_status`.

---

##  How the OOP code talks to MySQL

```
React (fetch JSON) ──► VeloraServer (HTTP, port 8080)
                          │
                          ▼
                  ReservationService  ── one JDBC transaction ──┐
                  │      │       │       │                     │
               RoomDAO GuestDAO BookingDAO PaymentDAO          │
                  │                                            │
          RoomFactory → StandardRoom / DeluxeRoom / PenthouseSuite
                         calculateTariff()  (polymorphism)     │
                                                               ▼
                                     DBConnection → MySQL velora_db
```

| Endpoint | What it does |
| --- | --- |
| `GET /api/suites` | Rooms + types + amenities (`RoomDAO.findAll`) |
| `GET /api/quote?room=201&nights=2` | Bill from the Room subclass's `calculateTariff()` |
| `POST /api/reserve` | Locks the room (`SELECT … FOR UPDATE`), saves guest, booking, payment, marks room occupied, `COMMIT` |
| `GET /api/bookings` | Ledger from the `v_booking_details` view |
| `POST /api/checkout` | Calls the stored procedure `sp_checkout` through a `CallableStatement` |
| `GET /api/stats` | Revenue per tier from `v_revenue_by_type` |

---

##  Package & Class Structure

```text
src/
├── VeloraServer.java          REST API for the React frontend
├── Main.java                  Console front desk
└── velora/
    ├── dao/                   All SQL lives here
    │   ├── BookingDAO.java
    │   ├── GuestDAO.java
    │   ├── PaymentDAO.java
    │   ├── ReportDAO.java
    │   └── RoomDAO.java
    ├── exception/
    │   ├── InvalidBookingException.java
    │   └── RoomNotAvailableException.java
    ├── model/
    │   ├── Booking.java
    │   ├── DeluxeRoom.java
    │   ├── Guest.java
    │   ├── PenthouseSuite.java
    │   ├── Room.java           abstract: calculateTariff(), getTier()
    │   ├── RoomFactory.java
    │   └── StandardRoom.java
    ├── payment/
    │   ├── CardPayment.java
    │   ├── CashPayment.java
    │   ├── Payable.java        interface
    │   ├── Payment.java        abstract
    │   └── UpiPayment.java
    ├── service/
    │   ├── DBConnection.java   reads db.properties
    │   ├── HotelEngine.java
    │   ├── InvoiceService.java
    │   ├── LoggerService.java
    │   └── ReservationService.java   the transaction
    └── web/
        └── Json.java
database/
└── velora_schema.sql
```
