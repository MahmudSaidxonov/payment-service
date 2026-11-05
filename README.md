# Payment Service

Payment processing backend built with Java 17, Spring Boot 3, and PostgreSQL 16.

## Tech Stack
- **Java 17** & **Spring Boot 3.4** (Web, Data JPA, Security, Validation, Actuator)
- **PostgreSQL 16** (Auto-initialized on startup)
- **JWT Security** (Access + Refresh token rotation, BCrypt)
- **Swagger UI** (`springdoc-openapi`)
- **JUnit 5 & Testcontainers** (Integration testing with PostgreSQL)
- **Docker** (Multi-stage build)

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | PostgreSQL host | - |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name (created automatically if missing) | `payment` |
| `DB_USER` | Database user | - |
| `DB_PASSWORD` | Database password | - |
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | - |
| `JWT_ACCESS_TTL` | Access token TTL in seconds | `900` |
| `JWT_REFRESH_TTL` | Refresh token TTL in seconds | `604800` |
| `SERVER_PORT` | Server HTTP port | `8080` |
| `WEBHOOK_URL` | Webhook URL for final payment statuses | empty |

- **Webhook Notifications:** For quick testing, grab a free temporary URL from `https://webhook.site` and pass it via `-e WEBHOOK_URL=https://webhook.site/<your-id>` when starting `payment-app`.

## Running with Docker

### 1) Create a Docker network
```bash
docker network create payment-net
```

### 2) Create a volume for PostgreSQL
```bash
docker volume create payment-pgdata
```

### 3) Start PostgreSQL 16
```bash
docker run -d --name payment-db \
  --network payment-net \
  -e POSTGRES_USER=payment_user \
  -e POSTGRES_PASSWORD=change_me \
  -v payment-pgdata:/var/lib/postgresql/data \
  --health-cmd="pg_isready -U payment_user -d postgres" \
  --health-interval=5s --health-retries=10 \
  postgres:16
```
`DatabaseInitializer` automatically connects to PostgreSQL on startup (with up to 10 retries), creates the `payment` database if it does not exist, initializes the schema, and seeds the error codes.

### 4) Build the application image
```bash
docker build -t payment-service:1.0 .
```

### 5) Run the application container
```bash
docker run -d --name payment-app \
  --network payment-net \
  -p 8080:8080 \
  -e DB_HOST=payment-db \
  -e DB_PORT=5432 \
  -e DB_NAME=payment \
  -e DB_USER=payment_user \
  -e DB_PASSWORD=change_me \
  -e JWT_SECRET=my_super_secret_jwt_key_at_least_32_characters_long \
  payment-service:1.0
```

### Stop and Clean Up
```bash
docker stop payment-app payment-db
docker rm payment-app payment-db
docker network rm payment-net
```

## Endpoints & Monitoring
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Health Check:** `http://localhost:8080/actuator/health`
- **Prometheus Metrics:** `http://localhost:8080/actuator/prometheus`

## Example cURL Requests

### 1) Register a user
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mahmud_sa",
    "password": "password962"
  }'
```

### 2) Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mahmud_sa",
    "password": "password962"
  }'
```

### 3) Refresh tokens
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<YOUR_REFRESH_TOKEN>"
  }'
```

### 4) Create a payment
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 290000.00,
    "currency": "UZS",
    "receiverAccount": "20208000900000001234",
    "description": "Internet payment"
  }'
```

### 5) Get payment details and status history
```bash
curl -X GET "http://localhost:8080/api/v1/payments/<PAYMENT_UUID>?lang=en" \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### 6) List payments with pagination
```bash
curl -X GET "http://localhost:8080/api/v1/payments?page=0&size=20&status=SUCCESS" \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### 7) Cancel a NEW payment
```bash
curl -X POST http://localhost:8080/api/v1/payments/<PAYMENT_UUID>/cancel \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### 8) Get error codes
```bash
curl -X GET "http://localhost:8080/api/v1/error-codes?lang=ru&category=PAYMENT"
```

## Running Tests
```bash
mvn test
```

## Additional Features
- **Payment Cancellation:** `POST /api/v1/payments/{id}/cancel` with pessimistic locking (`PESSIMISTIC_WRITE`).
- **Login Rate Limiting:** 5 requests per minute limit on `/api/v1/auth/login` (`429 Too Many Requests`).
- **Webhook Notifications:** Sends a `POST` request to `WEBHOOK_URL` when a payment reaches `SUCCESS` or `FAILED`.
- **Prometheus Metrics:** Available at `/actuator/prometheus`.
- **CI Pipeline:** GitHub Actions workflow (`.github/workflows/ci.yml`) runs build and Testcontainers integration tests on push.