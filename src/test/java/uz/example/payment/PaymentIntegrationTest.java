package uz.example.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(properties = {
        "JWT_SECRET=my_super_puper_jwt_secret_key_just_in_case"
})
@AutoConfigureMockMvc
@Testcontainers
public class PaymentIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private  MockMvc mockMvc;
    @Autowired
    private  ObjectMapper objectMapper;

    @Test
    void shouldReturnErrorCodesInRequestedLanguage() throws Exception {

        mockMvc.perform(get("/api/v1/error-codes")
                        .param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(17))
                .andExpect(jsonPath("$[0].code").value(1001));

        mockMvc.perform(get("/api/v1/error-codes/2003")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.category").value("PAYMENT"))
                .andExpect(jsonPath("$.httpStatus").value(422));

        mockMvc.perform(get("/api/v1/error-codes/2003")
                        .param("lang", "uz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.httpStatus").value(422))
                .andExpect(jsonPath("$.message").value("Hisobda mablag' yetarli emas"));

        mockMvc.perform(get("/api/v1/error-codes/2003")
                        .header("Accept-Language", "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/v1/error-codes/8888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(9002));
    }

    @Test
    void shouldCompleteFullPaymentFlowTest() throws Exception {

        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "test_user",
                "password", "Password962"
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.password").doesNotExist());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        String idempotencyKey = UUID.randomUUID().toString();
        String paymentBody = objectMapper.writeValueAsString(Map.of(
                "amount", 190000.00,
                "currency", "UZS",
                "receiverAccount", "20208000920010001962",
                "description", "Test payment"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        String paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId));

        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history[0].status").value("NEW"));
    }

    @Test

    void shouldReturn404accessForeignPaymentTest() throws Exception {
        String user1Token = registerAndLogin("test_user_one", "Password962");
        String user2Token = registerAndLogin("test_user_two", "Password252");

        String paymentBody = objectMapper.writeValueAsString(Map.of(
                "amount", 50000.00,
                "currency", "USD",
                "receiverAccount", "20208000900010005678",
                "description", "First user's payment"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + user1Token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2006));
    }

    private String registerAndLogin(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
