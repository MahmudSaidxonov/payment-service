package uz.example.payment.payment.entity;

public enum PaymentStatus {
    NEW,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case NEW -> next == PROCESSING || next == CANCELED;
            case PROCESSING -> next == SUCCESS || next == FAILED;
            case SUCCESS, FAILED, CANCELED -> false;
        };
    }
}
