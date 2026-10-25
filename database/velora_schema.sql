-- =====================================================================
--  VELORA GRAND — velora_db
--  8 tables in 3NF, 3 views, 1 stored procedure, 1 function, 2 triggers
--
--  Run this whole file in MySQL Workbench (lightning icon) or:
--      mysql -u root -p < database/velora_schema.sql
--
--  WARNING: it drops and recreates velora_db, so old test bookings are lost.
-- =====================================================================

DROP DATABASE IF EXISTS velora_db;
CREATE DATABASE velora_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE velora_db;

-- ---------------------------------------------------------------------
-- 1. room_types : one row per category (Standard, Deluxe, Penthouse).
--    type_name decides which Java subclass of Room is created.
-- ---------------------------------------------------------------------
CREATE TABLE room_types (
    type_id     INT          PRIMARY KEY AUTO_INCREMENT,
    type_name   VARCHAR(20)  NOT NULL UNIQUE,
    bed_type    VARCHAR(30)  NOT NULL,
    max_guests  TINYINT      NOT NULL CHECK (max_guests BETWEEN 1 AND 8),
    tagline     VARCHAR(120) NOT NULL
);

-- ---------------------------------------------------------------------
-- 2. rooms : one row per physical suite.
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
    room_number      INT           PRIMARY KEY,
    type_id          INT           NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    description      VARCHAR(300)  NOT NULL,
    image_url        VARCHAR(300)  NOT NULL,
    price_per_night  DECIMAL(10,2) NOT NULL CHECK (price_per_night > 0),
    status           ENUM('AVAILABLE','OCCUPIED','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_rooms_type FOREIGN KEY (type_id) REFERENCES room_types(type_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- 3. amenities + 4. room_amenities : many-to-many (a room has many
--    amenities, an amenity belongs to many rooms). Keeps rooms in 1NF.
-- ---------------------------------------------------------------------
CREATE TABLE amenities (
    amenity_id  INT         PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE room_amenities (
    room_number INT NOT NULL,
    amenity_id  INT NOT NULL,
    PRIMARY KEY (room_number, amenity_id),
    CONSTRAINT fk_ra_room    FOREIGN KEY (room_number) REFERENCES rooms(room_number)  ON DELETE CASCADE,
    CONSTRAINT fk_ra_amenity FOREIGN KEY (amenity_id)  REFERENCES amenities(amenity_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 5. guests : stored once; a returning guest is matched by phone.
-- ---------------------------------------------------------------------
CREATE TABLE guests (
    guest_id    INT          PRIMARY KEY AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(15)  NOT NULL UNIQUE,
    email       VARCHAR(120) UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 6. bookings : one row per reservation. Nights are not stored —
--    they are derived from the dates (DATEDIFF), which keeps 3NF.
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id    INT           PRIMARY KEY AUTO_INCREMENT,
    booking_ref   CHAR(9)       NOT NULL UNIQUE,
    guest_id      INT           NOT NULL,
    room_number   INT           NOT NULL,
    check_in      DATE          NOT NULL,
    check_out     DATE          NOT NULL,
    total_amount  DECIMAL(12,2) NOT NULL CHECK (total_amount > 0),
    status        ENUM('CONFIRMED','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dates CHECK (check_out > check_in),
    CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id)    REFERENCES guests(guest_id)    ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_room  FOREIGN KEY (room_number) REFERENCES rooms(room_number)  ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- 7. payments : how a booking was settled (one booking, one or more payments).
--    Only the last 4 card digits or the UPI ID are kept in `reference`.
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id  INT           PRIMARY KEY AUTO_INCREMENT,
    booking_id  INT           NOT NULL,
    method      ENUM('CARD','CASH','UPI') NOT NULL,
    amount      DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    reference   VARCHAR(60),
    paid_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 8. booking_audit : filled only by triggers — every status change of a booking.
-- ---------------------------------------------------------------------
CREATE TABLE booking_audit (
    audit_id    INT         PRIMARY KEY AUTO_INCREMENT,
    booking_id  INT         NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- =====================================================================
--  TRIGGERS
-- =====================================================================
DELIMITER //
CREATE TRIGGER trg_booking_created AFTER INSERT ON bookings
FOR EACH ROW
BEGIN
    INSERT INTO booking_audit (booking_id, old_status, new_status)
    VALUES (NEW.booking_id, NULL, NEW.status);
END //

CREATE TRIGGER trg_booking_status AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO booking_audit (booking_id, old_status, new_status)
        VALUES (NEW.booking_id, OLD.status, NEW.status);
    END IF;
END //
DELIMITER ;

-- =====================================================================
--  FUNCTION + STORED PROCEDURE
-- =====================================================================
DELIMITER //
CREATE FUNCTION fn_stay_nights(p_check_in DATE, p_check_out DATE)
RETURNS INT DETERMINISTIC
BEGIN
    RETURN DATEDIFF(p_check_out, p_check_in);
END //

-- Checkout as one transaction: close the booking and free the room.
CREATE PROCEDURE sp_checkout(IN p_booking_ref CHAR(9))
BEGIN
    DECLARE v_room INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    SELECT room_number INTO v_room FROM bookings
    WHERE booking_ref = p_booking_ref AND status = 'CONFIRMED'
    FOR UPDATE;

    IF v_room IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No confirmed booking with that reference';
    END IF;

    UPDATE bookings SET status = 'CHECKED_OUT' WHERE booking_ref = p_booking_ref;
    UPDATE rooms    SET status = 'AVAILABLE'   WHERE room_number = v_room;
    COMMIT;
END //
DELIMITER ;

-- =====================================================================
--  VIEWS
-- =====================================================================
CREATE VIEW v_booking_details AS
SELECT b.booking_ref, g.full_name AS guest_name, g.phone, g.email,
       r.room_number, r.name AS suite_name, t.type_name AS tier,
       b.check_in, b.check_out, fn_stay_nights(b.check_in, b.check_out) AS nights,
       b.total_amount, p.method AS payment_method, p.reference AS payment_reference,
       b.status, b.created_at
FROM bookings b
JOIN guests g     ON g.guest_id = b.guest_id
JOIN rooms r      ON r.room_number = b.room_number
JOIN room_types t ON t.type_id = r.type_id
LEFT JOIN payments p ON p.booking_id = b.booking_id;

CREATE VIEW v_revenue_by_type AS
SELECT t.type_name AS tier,
       COUNT(b.booking_id)              AS total_bookings,
       COALESCE(SUM(b.total_amount), 0) AS revenue
FROM room_types t
JOIN rooms r         ON r.type_id = t.type_id
LEFT JOIN bookings b ON b.room_number = r.room_number AND b.status <> 'CANCELLED'
GROUP BY t.type_id, t.type_name;

CREATE VIEW v_available_rooms AS
SELECT r.room_number, r.name, t.type_name AS tier, r.price_per_night
FROM rooms r JOIN room_types t ON t.type_id = r.type_id
WHERE r.status = 'AVAILABLE';

-- =====================================================================
--  SEED DATA
-- =====================================================================
INSERT INTO room_types (type_name, bed_type, max_guests, tagline) VALUES
('Standard',  'Queen bed', 2, 'Quiet, modern comfort'),
('Deluxe',    'King bed',  3, 'Skyline views and VIP concierge'),
('Penthouse', 'Emperor bed', 4, 'Rooftop privacy with a personal butler');

INSERT INTO rooms (room_number, type_id, name, description, image_url, price_per_night) VALUES
(101, 1, 'Executive Studio Suite',
 'A minimalist sanctuary featuring acoustic soundproofing, an ergonomic workstation, high-speed fiber Wi-Fi and a rainfall marble shower.',
 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80', 10000),
(102, 1, 'Garden Courtyard Suite',
 'Opens onto a private courtyard garden with a reading nook, herbal tea bar and soft natural light all day.',
 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=80', 12000),
(201, 2, 'Skyline Panorama Suite',
 'Floor-to-ceiling panoramic glass offering skyline vistas, complimentary champagne breakfast and a deep-soaking bath.',
 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80', 18500),
(202, 2, 'Riverside Grand Suite',
 'A corner suite above the river with a private balcony lounge, walk-in wardrobe and a Nespresso bar.',
 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80', 24000),
(301, 3, 'Imperial Presidential Sanctuary',
 'The pinnacle of bespoke luxury: a private rooftop infinity jacuzzi, 24/7 dedicated butler, curated mini-bar and private elevator access.',
 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 55000);

INSERT INTO amenities (name) VALUES
('Fiber Wi-Fi'), ('Smart 4K TV'), ('Espresso Bar'), ('Rainfall Shower'),
('Garden Access'), ('Skyline Balcony'), ('Gourmet Breakfast'), ('Marble Bath'),
('Private Butler'), ('Curated Mini-Bar'), ('Infinity Spa'), ('Private Elevator');

INSERT INTO room_amenities (room_number, amenity_id) VALUES
(101, 1), (101, 2), (101, 3), (101, 4),
(102, 1), (102, 2), (102, 5),
(201, 1), (201, 6), (201, 7), (201, 8),
(202, 1), (202, 3), (202, 6), (202, 7),
(301, 1), (301, 9), (301, 10), (301, 11), (301, 12);

-- =====================================================================
--  QUERIES TO SHOW IN THE VIVA
-- =====================================================================
-- SELECT * FROM v_booking_details ORDER BY created_at DESC;
-- SELECT * FROM v_revenue_by_type;
-- SELECT * FROM v_available_rooms;
-- SELECT * FROM booking_audit ORDER BY changed_at DESC;
-- CALL sp_checkout('VEL-12345');
--
-- Guests who stayed more than once:
-- SELECT g.full_name, COUNT(*) AS stays FROM guests g
-- JOIN bookings b ON b.guest_id = g.guest_id GROUP BY g.guest_id HAVING COUNT(*) > 1;
--
-- Amenities of every suite (many-to-many join):
-- SELECT r.room_number, GROUP_CONCAT(a.name ORDER BY a.name SEPARATOR ', ') AS amenities
-- FROM rooms r JOIN room_amenities ra ON ra.room_number = r.room_number
-- JOIN amenities a ON a.amenity_id = ra.amenity_id GROUP BY r.room_number;
