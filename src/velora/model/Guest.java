package velora.model;

public class Guest {
    private final String guestId;
    private final String fullName;
    private final String phone;

    public Guest(String guestId, String fullName, String phone) {
        this.guestId = guestId;
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getGuestId() { return guestId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
}