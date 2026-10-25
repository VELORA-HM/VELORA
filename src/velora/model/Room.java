package velora.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Room {
    private final int roomNumber;
    private final String name;
    private final double basePrice;
    private boolean isBooked;
    private String description = "";
    private String imageUrl = "";
    private List<String> amenities = new ArrayList<>();

    public Room(int roomNumber, double basePrice) {
        this(roomNumber, "Suite " + roomNumber, basePrice);
    }

    public Room(int roomNumber, String name, double basePrice) {
        this.roomNumber = roomNumber;
        this.name = name;
        this.basePrice = basePrice;
        this.isBooked = false;
    }

    /** Each tier bills differently: this is the polymorphic method the whole system relies on. */
    public abstract double calculateTariff(int nights);

    /** Tier name exactly as stored in room_types.type_name. */
    public abstract String getTier();

    /** Human-readable pricing rule, shown in the booking form. */
    public abstract String getTariffRule();

    public void book() { this.isBooked = true; }
    public void vacate() { this.isBooked = false; }

    public void setDetails(String description, String imageUrl, List<String> amenities) {
        this.description = description;
        this.imageUrl = imageUrl;
        this.amenities = new ArrayList<>(amenities);
    }

    public int getRoomNumber() { return roomNumber; }
    public String getName() { return name; }
    public double getBasePrice() { return basePrice; }
    public boolean isBooked() { return isBooked; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getAmenities() { return amenities; }
}
