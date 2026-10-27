package uz.example.payment.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse (
    Instant timestamp,
    int status,
    int code,
    String message,
    String path,
    String traceId,
    List<FieldErrorDetail> details
){
    public record FieldErrorDetail (
       String field,
       String reason
    ){}
}
