package velora.payment;

public interface Payable {
    boolean processPayment(double amount);
    String getPaymentMethod();
}