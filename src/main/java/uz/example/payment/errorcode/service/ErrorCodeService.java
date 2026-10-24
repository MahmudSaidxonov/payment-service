package uz.example.payment.errorcode.service;

import io.swagger.v3.oas.annotations.servers.Server;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uz.example.payment.common.BusinessException;
import uz.example.payment.errorcode.dto.ErrorCodeResponse;
import uz.example.payment.errorcode.entity.ErrorCategory;
import uz.example.payment.errorcode.entity.ErrorCode;
import uz.example.payment.errorcode.repository.ErrorCodeRepository;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class ErrorCodeService {

    private final ErrorCodeRepository errorCodeRepository;

    public List<ErrorCodeResponse> getAll(String categoryStr, String langParam, String acceptLanguage) {
        String lang = resolveLanguage(langParam, acceptLanguage);
        List<ErrorCode> codes = List.of();

        if (categoryStr != null && !categoryStr.isBlank()) {
            ErrorCategory category = parseCategory(categoryStr);
            codes = errorCodeRepository.findAllByCategoryOrderByCodeAsc(category);
        } else {
            codes = errorCodeRepository.findAllByOrderByCodeAsc();
        }

        return codes.stream()
                .map(errorCode -> toDto(errorCode, lang))
                .toList();
    }

    public ErrorCodeResponse getByCode(int code, String langParam, String acceptLanguage) {
        String lang = resolveLanguage(langParam, acceptLanguage);

        ErrorCode errorCode = errorCodeRepository.findById(code)
                .orElseThrow(() -> new BusinessException(9002));

        return toDto(errorCode, lang);
    }

    public String resolveLanguage(String langParam, String acceptLanguage) {
        String inputLanguage = (langParam != null && !langParam.isBlank())
                ? langParam : acceptLanguage;

        if (inputLanguage == null || inputLanguage.isBlank()) {
            return "uz";
        }

        String lowered = inputLanguage.trim().toLowerCase();

        if (lowered.startsWith("ru")) {
            return "ru";
        }

        if (lowered.startsWith("en")) {
            return "en";
        }

        return "uz";
    }

    private ErrorCategory parseCategory(String categoryStr) {
        try {
            return ErrorCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(9001);
        }
    }

    private ErrorCodeResponse toDto(ErrorCode ec, String lang) {
        String message =  switch (lang) {
            case "ru" -> ec.getMessageRu();
            case "en" -> ec.getMessageEn();
                default -> ec.getMessageUz();
        };
        return new ErrorCodeResponse(
                ec.getCode(),
                ec.getCategory(),
                ec.getHttpStatus(),
                message
        );
    }
}
