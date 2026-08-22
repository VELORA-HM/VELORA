package velora.model;

public abstract class Room {
    private final int roomNumber;
    private final double basePrice;
    private boolean isBooked;

    public Room(int roomNumber, double basePrice) {
        this.roomNumber = roomNumber;
        this.basePrice = basePrice;
        this.isBooked = false;
    }

    public abstract double calculateTariff(int nights);

    public void book() { this.isBooked = true; }
    public void vacate() { this.isBooked = false; }

    public int getRoomNumber() { return roomNumber; }
    public double getBasePrice() { return basePrice; }
    public boolean isBooked() { return isBooked; }
}