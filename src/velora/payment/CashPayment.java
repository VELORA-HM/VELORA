package velora.payment;

public class CashPayment extends Payment {
    private final double cashTendered;

    public CashPayment(String transactionId, double amount, double cashTendered) {
        super(transactionId, amount);
        this.cashTendered = cashTendered;
    }

    @Override
    public boolean processPayment(double amount) {
        if (cashTendered >= amount) {
            this.isSettled = true;
            return true;
        }
        return false;
    }

    public double getChange() { return Math.max(0, cashTendered - amount); }

    @Override
    public String getPaymentMethod() { return "Cash Tendered"; }

    @Override
    public String generateReceipt() {
        return String.format("[RECEIPT] TXN: %s | Amount: $%.2f | Tendered: $%.2f | Change Returned: $%.2f",
                transactionId, amount, cashTendered, getChange());
    }
}