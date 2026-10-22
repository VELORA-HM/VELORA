package velora.model;

public class PenthouseSuite extends Room {
    private static final double PENTHOUSE_TAX = 0.18; // 18% Luxury Tax
    private static final double BUTLER_SERVICE_FEE = 100.0;
    private double miniBarCharges = 0.0;

    public PenthouseSuite(int roomNumber, double basePrice) {
        super(roomNumber, basePrice);
    }

    public void addMiniBarUsage(double amount) {
        if (amount > 0) {
            this.miniBarCharges += amount;
        }
    }

    @Override
    public double calculateTariff(int nights) {
        double baseTotal = getBasePrice() * nights;
        double subtotal = baseTotal + (baseTotal * PENTHOUSE_TAX) + (BUTLER_SERVICE_FEE * nights);
        return subtotal + miniBarCharges;
    }

    @Override
    public void vacate() {
        super.vacate();
        this.miniBarCharges = 0.0; // Reset mini-bar tabs on checkout
    }

    public double getMiniBarCharges() {
        return miniBarCharges;
    }

    @Override
    public String toString() {
        return String.format("[👑 Presidential Penthouse #%d] Base: ₹%.2f/night (+18%% Tax + Butler Concierge) | Status: %s",
                getRoomNumber(), getBasePrice(), isBooked() ? "OCCUPIED" : "AVAILABLE");
    }
}