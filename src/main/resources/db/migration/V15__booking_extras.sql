ALTER TABLE bookings ADD COLUMN pickup_location  VARCHAR(100);
ALTER TABLE bookings ADD COLUMN return_location  VARCHAR(100);
ALTER TABLE bookings ADD COLUMN gps_navigator    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN child_seat       BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN driver_service   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN booking_number   VARCHAR(20);

CREATE UNIQUE INDEX idx_bookings_booking_number
    ON bookings (booking_number)
    WHERE booking_number IS NOT NULL;