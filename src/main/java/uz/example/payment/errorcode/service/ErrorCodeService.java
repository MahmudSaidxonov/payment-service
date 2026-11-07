package uz.example.payment.errorcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uz.example.payment.common.BusinessException;
import uz.example.payment.errorcode.dto.ErrorCodeResponse;
import uz.example.payment.errorcode.entity.ErrorCategory;
import uz.example.payment.errorcode.entity.ErrorCode;
import uz.example.payment.errorcode.repository.ErrorCodeRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class ErrorCodeService {

    private final ErrorCodeRepository errorCodeRepository;

    public List<ErrorCodeResponse> getAll(String categoryStr, String langParam, String acceptLanguage) {
        String lang = resolveLanguage(langParam, acceptLanguage);
        List<ErrorCode> codes;

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
        if (langParam != null && !langParam.isBlank()) {
            return parseLangCode(langParam);
        }

        if (acceptLanguage == null || acceptLanguage.isBlank()
                || acceptLanguage.contains(";q=") || acceptLanguage.contains(",")) {
            return "uz";
        }

        return parseLangCode(acceptLanguage);
    }

    public ErrorCode getEntityByCode(int code) {
        return errorCodeRepository.findById(code)
                .orElseThrow(() -> new IllegalStateException("Error code not found in DB: " + code));
    }

    private String parseLangCode(String input) {
        String lowered = input.trim().toLowerCase();
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
        return new ErrorCodeResponse(
                ec.getCode(),
                ec.getCategory(),
                ec.getHttpStatus(),
                ec.getMessageByLang(lang)
        );
    }
}
