package velora.payment;

public abstract class Payment implements Payable {
    protected final String transactionId;
    protected final double amount;
    protected boolean isSettled;

    public Payment(String transactionId, double amount) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.isSettled = false;
    }

    public abstract String generateReceipt();

    /** Value stored in payments.method: CARD, CASH or UPI. */
    public abstract String getMethodCode();

    /** Safe reference stored in payments.reference (masked card, UPI ID, or null). */
    public abstract String getReference();

    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public boolean isSettled() { return isSettled; }
}
