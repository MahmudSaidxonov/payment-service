package uz.example.payment.bootstrap;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uz.example.payment.errorcode.entity.ErrorCategory;
import uz.example.payment.errorcode.entity.ErrorCode;
import uz.example.payment.errorcode.repository.ErrorCodeRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorCodeSeeder implements ApplicationRunner {

    private final ErrorCodeRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<ErrorCode> initialCodes = List.of(
                new ErrorCode(1001, ErrorCategory.AUTH, 401,
                        "Login yoki parol noto'g'ri",
                        "Неверный логин или пароль",
                        "Invalid username or password"),
                new ErrorCode(1002, ErrorCategory.AUTH, 401,
                        "Token muddati tugagan",
                        "Срок действия токена истек",
                        "Token has expired"),
                new ErrorCode(1003, ErrorCategory.AUTH, 401,
                        "Token yaroqsiz",
                        "Недействительный токен",
                        "Invalid token"),
                new ErrorCode(1004, ErrorCategory.AUTH, 409,
                        "Bunday username allaqachon mavjud",
                        "Такой username уже существует",
                        "Username already exists"),
                new ErrorCode(1005, ErrorCategory.AUTH, 401,
                        "Refresh token yaroqsiz yoki ishlatilgan",
                        "Refresh-токен недействителен или уже использован",
                        "Refresh token is invalid or already used"),

                new ErrorCode(2001, ErrorCategory.PAYMENT, 400,
                        "Summa noto'g'ri",
                        "Некорректная сумма",
                        "Invalid amount"),
                new ErrorCode(2002, ErrorCategory.PAYMENT, 400,
                        "Valyuta qo'llab-quvvatlanmaydi",
                        "Валюта не поддерживается",
                        "Currency is not supported"),
                new ErrorCode(2003, ErrorCategory.PAYMENT, 422,
                        "Hisobda mablag' yetarli emas",
                        "Недостаточно средств на счете",
                        "Insufficient funds"),
                new ErrorCode(2004, ErrorCategory.PAYMENT, 422,
                        "Qabul qiluvchi hisobi bloklangan",
                        "Счет получателя заблокирован",
                        "Receiver account is blocked"),
                new ErrorCode(2005, ErrorCategory.PAYMENT, 504,
                        "Qayta ishlash vaqti tugadi",
                        "Время обработки истекло",
                        "Processing timed out"),
                new ErrorCode(2006, ErrorCategory.PAYMENT, 404,
                        "To'lov topilmadi",
                        "Платеж не найден",
                        "Payment not found"),
                new ErrorCode(2007, ErrorCategory.PAYMENT, 400,
                        "Idempotency-Key yuborilmagan yoki noto'g'ri",
                        "Idempotency-Key отсутствует или некорректен",
                        "Idempotency-Key is missing or invalid"),
                new ErrorCode(2008, ErrorCategory.PAYMENT, 409,
                        "Idempotency-Key boshqa ma'lumot bilan ishlatilgan",
                        "Idempotency-Key уже использован с другими данными",
                        "Idempotency-Key already used with different payload"),

                new ErrorCode(9001, ErrorCategory.COMMON, 400,
                        "Validatsiya xatosi",
                        "Ошибка валидации",
                        "Validation error"),
                new ErrorCode(9002, ErrorCategory.COMMON, 404,
                        "Resurs topilmadi",
                        "Ресурс не найден",
                        "Resource not found"),
                new ErrorCode(9003, ErrorCategory.COMMON, 403,
                        "Ruxsat yo'q",
                        "Доступ запрещен",
                        "Access denied"),
                new ErrorCode(9999, ErrorCategory.COMMON, 500,
                        "Ichki server xatosi",
                        "Внутренняя ошибка сервера",
                        "Internal server error")
        );

//        int count = 0;
//        for (ErrorCode code : initialCodes) {
//            if (!repository.existsById(code.getCode())) {
//                repository.save(code);
//                count++;
//            }
//        }

        Set<Integer> existingCodes = repository.findAll().stream()
                        .map(ErrorCode::getCode)
                                .collect(Collectors.toSet());

        List<ErrorCode> toInsert = initialCodes.stream()
                        .filter(errorCode -> !existingCodes.contains(errorCode.getCode()))
                                .toList();

        if (!toInsert.isEmpty()) {
            repository.saveAll(toInsert);
        }

        log.info("ErrorCodeSeeder completed. Inserted {} new error codes.", toInsert.size());
    }
}
