package uz.example.payment.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code) {
        super("Business error code: " + code);
        this.code = code;
    }

}
