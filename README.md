# wooricard-gateway

Spring Cloud Gateway 기반 API Gateway 서비스

---

## 아키텍처

```
Client
  │
  ▼
POST /auth/token  →  JWT 발급
  │
  ▼
API Gateway (8080)  ──── Eureka Server (8761)
  │  Authorization: Bearer <token> 검증
  │
  ├── /api/authorizations/**  ──► 승인/결제 서비스 (8081)
  ├── /api/settlement/**      ──► 정산 서비스     (8082)
  └── /api/billing/**         ──► 매입 청구 서비스 (8083)
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

## Swagger UI

게이트웨이 실행 후 아래 주소에서 API 문서를 확인할 수 있습니다.

```
http://192.168.1.249:8080/swagger-ui.html
```

---

## API 테스트 (APIdog)

### Step 1 — Environment 변수 설정

APIdog 좌측 **Environments → New Environment** 생성 후 아래 변수 추가

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

**Post-response Script** (응답 후 자동으로 토큰 저장)

```javascript
var res = pm.response.json();
pm.environment.set("access_token", res.access_token);
```

**응답 예시**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer"
}
```

---

### Step 3 — 서비스 요청

모든 요청의 **Headers** 에 아래 값 추가

| Key | Value |
|---|---|
| `Authorization` | `Bearer {{access_token}}` |

**승인/결제 서비스 엔드포인트**

| Method | URL | 설명 |
|--------|-----|------|
| GET | `{{base_url}}/api/authorizations` | 승인 내역 조회 |
| POST | `{{base_url}}/api/authorizations/request` | 카드 승인 요청 |
| POST | `{{base_url}}/api/authorizations/cancel` | 승인 취소 |
| GET | `{{base_url}}/api/authorization/approved/monthly` | 월별 승인 내역 조회 |

**정산/매입 서비스 엔드포인트**

| Method | URL | 설명 |
|--------|-----|------|
| GET | `{{base_url}}/api/settlement/...` | 정산 서비스 |
| GET | `{{base_url}}/api/billing/...` | 매입 청구 서비스 |

> 토큰 만료(1시간) 시 Step 2를 다시 실행하면 `access_token`이 자동 갱신됩니다.

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
