package velora.model;

public class Guest {
    private int guestId;          // 0 until saved; then guests.guest_id
    private final String fullName;
    private final String phone;
    private final String email;   // optional

    public Guest(String fullName, String phone, String email) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = (email == null || email.isBlank()) ? null : email;
    }

    public int getGuestId() { return guestId; }
    public void setGuestId(int guestId) { this.guestId = guestId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
