package velora.payment;

public class UpiPayment extends Payment {
    private final String upiId;

    public UpiPayment(String transactionId, double amount, String upiId) {
        super(transactionId, amount);
        this.upiId = upiId;
    }

    @Override
    public boolean processPayment(double amount) {
        if (upiId == null || !upiId.contains("@")) return false;   // name@bank
        this.isSettled = true;
        return true;
    }

    @Override
    public String getPaymentMethod() { return "UPI (" + upiId + ")"; }

    @Override
    public String getMethodCode() { return "UPI"; }

    @Override
    public String getReference() { return upiId; }

    @Override
    public String generateReceipt() {
        return String.format("[RECEIPT] TXN: %s | Amount: ₹%.2f | Method: %s | Status: APPROVED",
                transactionId, amount, getPaymentMethod());
    }
}
