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
}