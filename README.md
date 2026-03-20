# wooricard-gateway

Spring Cloud Gateway 기반 API Gateway 서비스

---

## 아키텍처

```
Client
  │
  ├── POST /auth/token     → JWT 발급
  ├── GET  /auth/validate  → 토큰 유효성 확인
  │
  └── GET|POST /api/**  →  JWT 검증 → Eureka 조회 → 서비스 전달
                                          │
                          ┌───────────────┼───────────────┐
                          ▼               ▼               ▼
               approval-service  settlement-service  billing-service
                   (8081)             (8082)             (8083)
```

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.12 |
| Spring Cloud | 2025.0.0 |
| jjwt | 0.12.6 |
| springdoc-openapi | 2.8.5 |

---

## 실행 순서

```
1. wooricard-eureka     (8761)
2. wooricard-config     (8888)
3. wooricard-gateway    (8080)  ← 이 서비스
4. 각 마이크로서비스
```

```bash
./gradlew bootRun
```

---

## Gateway 엔드포인트

| Method | URL | 설명 | 인증 필요 |
|--------|-----|------|----------|
| POST | `/auth/token` | JWT 토큰 발급 | X |
| GET | `/auth/validate` | 토큰 유효성 확인 | O |

---

## 라우팅 규칙

요청 경로에 따라 Eureka에서 서비스를 찾아 자동으로 전달합니다.

| 경로 | Eureka 서비스명 |
|------|----------------|
| `/api/authorizations/**` | `wooricard-approval-service` |
| `/api/authorization/**` | `wooricard-approval-service` |
| `/api/settlement/**` | `wooricard-settlement-service` |
| `/api/billing/**` | `wooricard-billing-service` |

---

## API 테스트 (APIdog)

### Step 1 — Environment 변수 설정

**Environments → New Environment** 생성 후 아래 변수 추가

| Variable | Value |
|---|---|
| `base_url` | `http://192.168.1.249:8080` |
| `access_token` | (비워둠) |

생성 후 우측 상단에서 해당 환경 **활성화**

---

### Step 2 — 토큰 발급

**요청**

```
Method : POST
URL    : {{base_url}}/auth/token
```

**Body (JSON)**

```json
{
  "clientId": "vensa",
  "clientSecret": "vensa-secret-2026"
}
```

**Post-response Script** (토큰 자동 저장)

```javascript
var res = pm.response.json();
pm.environment.set("access_token", res.access_token);
```

---

### Step 3 — 토큰 유효성 확인

```
Method : GET
URL    : {{base_url}}/auth/validate
Header : Authorization: Bearer {{access_token}}
```

| 응답 | 의미 |
|------|------|
| `200 {"valid": true, "clientId": "vensa"}` | 정상 토큰 |
| `401 {"valid": false, ...}` | 만료 또는 잘못된 토큰 |

---

### Step 4 — 서비스 요청

모든 요청 Headers에 추가:

| Key | Value |
|---|---|
| `Authorization` | `Bearer {{access_token}}` |

이후 각 서비스의 엔드포인트는 **담당자의 Swagger UI**를 참고하세요.

| 서비스 | Swagger UI |
|--------|-----------|
| 승인/결제 | `http://192.168.1.{IP}:8081/swagger-ui.html` |
| 정산 | `http://192.168.1.{IP}:8082/swagger-ui.html` |
| 매입 청구 | `http://192.168.1.{IP}:8083/swagger-ui.html` |

> 토큰 만료(1시간) 시 Step 2를 다시 실행하면 `access_token`이 자동 갱신됩니다.

---

## Swagger UI

```
http://192.168.1.249:8080/swagger-ui.html
```

---

## 주의사항

와이파이 환경이 바뀌면 `application.yaml` 의 아래 두 항목을 현재 IP로 수정해야 합니다.

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://{Eureka서버IP}:8761/eureka/
  instance:
    ip-address: {이 서버 IP}
```

- Eureka 대시보드: `http://192.168.1.80:8761`
- Gateway: `http://192.168.1.249:8080`
