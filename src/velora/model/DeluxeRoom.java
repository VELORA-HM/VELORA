package velora.model;

public class DeluxeRoom extends Room {
    private static final double LUXURY_TAX_RATE = 0.12;
    private static final double AMENITIES_FEE = 35.0;

    public DeluxeRoom(int roomNumber, double basePrice) {
        super(roomNumber, basePrice);
    }

    @Override
    public double calculateTariff(int nights) {
        double subtotal = getBasePrice() * nights;
        return subtotal + (subtotal * LUXURY_TAX_RATE) + (AMENITIES_FEE * nights);
    }

    @Override
    public String toString() {
        return String.format("[Deluxe Suite #%d] Base: ₹%.2f/night (+12%% Tax & VIP Concierge) | Status: %s",
                getRoomNumber(), getBasePrice(), isBooked() ? "OCCUPIED" : "AVAILABLE");
    }
}