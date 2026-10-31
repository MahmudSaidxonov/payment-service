package uz.example.payment.errorcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.example.payment.errorcode.dto.ErrorCodeResponse;
import uz.example.payment.errorcode.service.ErrorCodeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/error-codes")
@RequiredArgsConstructor
public class ErrorCodeController {

    private final ErrorCodeService errorCodeService;

    @GetMapping
    public List<ErrorCodeResponse> getAll(
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String category,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return errorCodeService.getAll(category, lang, acceptLanguage);
    }

    @GetMapping("{code}")
    public ErrorCodeResponse getByCode(
            @PathVariable int code,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return errorCodeService.getByCode(code, lang, acceptLanguage);
    }
}
