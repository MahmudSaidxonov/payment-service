package uz.example.payment.errorcode.dto;

import uz.example.payment.errorcode.entity.ErrorCategory;

public record ErrorCodeResponse (
     int code,
     ErrorCategory category,
     int httpStatus,
     String message
){}
