package uz.example.payment.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.example.payment.errorcode.entity.ErrorCode;
import uz.example.payment.errorcode.service.ErrorCodeService;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorCodeService errorCodeService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business exception occurred: code={}, path={}", ex.getCode(), request.getRequestURI());

        return buildErrorResponse(ex.getCode(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorResponse.FieldErrorDetail(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed on path={}: {}", request.getRequestURI(), details);
        return buildErrorResponse(9001, request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected internal server error on path={}", request.getRequestURI(), ex);
        return buildErrorResponse(9999, request, List.of());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse (
            int code,
            HttpServletRequest request,
            List<ErrorResponse.FieldErrorDetail> details
    ) {
        String lang = errorCodeService.resolveLanguage(
                request.getParameter("lang"),
                request.getHeader("Accept-Language"));

        ErrorCode errorCode = errorCodeService.getEntityByCode(code);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getMessageByLang(lang),
                request.getRequestURI(),
                MDC.get(TraceIdFilter.TRACE_ID_KEY),
                details
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
}
