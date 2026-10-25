package velora.model;

public class StandardRoom extends Room {
    public StandardRoom(int roomNumber, double basePrice) {
        super(roomNumber, basePrice);
    }

    public StandardRoom(int roomNumber, String name, double basePrice) {
        super(roomNumber, name, basePrice);
    }

    @Override
    public double calculateTariff(int nights) {
        return getBasePrice() * nights;
    }

    @Override
    public String getTier() { return "Standard"; }

    @Override
    public String getTariffRule() { return "Room rate × nights"; }

    @Override
    public String toString() {
        return String.format("[Standard Suite #%d] Base: ₹%.2f/night | Status: %s",
                getRoomNumber(), getBasePrice(), isBooked() ? "OCCUPIED" : "AVAILABLE");
    }
}
