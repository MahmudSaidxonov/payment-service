package uz.example.payment.errorcode.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "error_codes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorCode {
    @Id
    @Column(name = "code", nullable = false)
    private Integer code;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private ErrorCategory category;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Column(name = "message_uz", nullable = false)
    private String messageUz;

    @Column(name = "message_ru", nullable = false)
    private String messageRu;

    @Column(name = "message_en", nullable = false)
    private String messageEn;

    public String getMessageByLang(String lang) {
        return switch (lang) {
            case "ru" -> this.messageRu;
            case "en" -> this.messageEn;
            default -> this.messageUz;
        };
    }
}
