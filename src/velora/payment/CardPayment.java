package velora.payment;

public class CardPayment extends Payment {
    private final String maskedCardNumber;

    public CardPayment(String transactionId, double amount, String rawCardNumber) {
        super(transactionId, amount);
        this.maskedCardNumber = "•••• " + rawCardNumber.substring(Math.max(0, rawCardNumber.length() - 4));
    }

    @Override
    public boolean processPayment(double amount) {
        this.isSettled = true;
        return true;
    }

    @Override
    public String getPaymentMethod() { return "Card (" + maskedCardNumber + ")"; }

    @Override
    public String generateReceipt() {
        return String.format("[RECEIPT] TXN: %s | Amount: ₹%.2f | Method: %s | Status: APPROVED",
                transactionId, amount, getPaymentMethod());
    }
}