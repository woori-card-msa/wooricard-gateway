# wooricard-gateway

Spring Cloud Gateway 기반 API Gateway 서비스

---

## 프로젝트 개요

카드 처리 MSA 시스템의 단일 진입점(API Gateway) 역할을 담당합니다.
클라이언트의 모든 요청을 받아 각 마이크로서비스로 라우팅하며, JWT 기반 인증을 통해 인가된 클라이언트만 접근할 수 있도록 합니다.

---

## 시스템 아키텍처

```
Client (vensa)
  │
  ▼
POST /auth/token  →  JWT 발급
  │
  ▼
API Gateway (8080)  ──── Eureka Server (8761)
  │  Authorization: Bearer <token> 검증
  │
  ├── /api/approval/**   ──► 승인/결제 서비스 (8081)
  ├── /api/settlement/** ──► 정산 서비스     (8082)
  └── /api/billing/**    ──► 매입 청구 서비스 (8083)
```

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.12 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Gateway | - |
| Spring Cloud Netflix Eureka Client | - |
| Spring Security (WebFlux) | - |
| jjwt | 0.12.6 |
| Lombok | - |

---

## 인증 방식 (JWT)

모든 API 요청은 JWT 토큰이 필요합니다. `/auth/token`을 통해 토큰을 발급받은 뒤, 이후 요청의 헤더에 포함해야 합니다.

### 인증 흐름

```
1. POST /auth/token  → JWT 발급
2. GET  /api/...     → Authorization: Bearer <token> 헤더 포함
3. 토큰 없음 / 만료  → 401 Unauthorized 반환
```

### 토큰 발급

```
POST http://192.168.1.249:8080/auth/token
Content-Type: application/json

{
  "clientId": "vensa",
  "clientSecret": "vensa-secret-2024"
}
```

응답:
```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer"
}
```

### API 요청 시 헤더

```
Authorization: Bearer eyJ...
```

> `clientId`, `clientSecret`은 `application.yaml`의 `jwt.client` 항목에서 관리합니다.
> 토큰 만료 시간은 기본 1시간이며 `jwt.expiration-ms` 항목에서 변경 가능합니다.

---

## 라우팅 규칙

| 서비스 | 경로 패턴 | 연결 방식 (Eureka) | 연결 방식 (로컬) |
|--------|----------|--------------------|-----------------|
| 승인/결제 서비스 | `/api/approval/**` | `lb://wooricard-approval-service` | `http://localhost:8081` |
| 정산 서비스 | `/api/settlement/**` | `lb://wooricard-settlement-service` | `http://localhost:8082` |
| 매입 청구 서비스 | `/api/billing/**` | `lb://wooricard-billing-service` | `http://localhost:8083` |

---

## 실행 방법

### 실행 순서 (운영/통합 환경)

```
1. wooricard-eureka     (port: 8761)
2. wooricard-config     (port: 8888)
3. wooricard-gateway    (port: 8080)  ← 이 서비스
4. 각 마이크로서비스 실행
```

### 실행 명령

```bash
./gradlew bootRun
```

IntelliJ: `Active profiles` 비워두고 실행

---

## API 테스트 (APIdog)

### Base URL

```
http://192.168.1.249:8080
```

---

### Step 1 — Environment 생성

APIdog 좌측 **Environments → New Environment** 생성 (이름 예: `wooricard-dev`)

| Variable | Value |
|---|---|
| `base_url` | `http://192.168.1.249:8080` |
| `access_token` | *(비워둠 — 자동으로 채워짐)* |

생성 후 우측 상단에서 `wooricard-dev` 환경을 **활성화** 합니다.

---

### Step 2 — 토큰 발급 요청 생성

새 요청을 생성합니다.

```
Method : POST
URL    : {{base_url}}/auth/token
```

**Body 탭 → JSON 선택:**
```json
{
  "clientId": "vensa",
  "clientSecret": "vensa-secret-2024"
}
```

**Post-response 탭 → 스크립트 입력:**
```javascript
var res = pm.response.json();
pm.environment.set("access_token", res.access_token);
```

요청을 보내면 응답이 오고, `access_token` 환경변수에 토큰이 자동 저장됩니다.

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer"
}
```

---

### Step 3 — 서비스 요청에 토큰 적용

승인/결제, 정산, 매입 청구 요청 **각각** 아래와 같이 설정합니다.

**Headers 탭에 추가:**

| Key | Value |
|---|---|
| `Authorization` | `Bearer {{access_token}}` |

`{{access_token}}`은 Step 2에서 저장된 환경변수를 자동으로 참조합니다.

**요청 URL 예시:**

| 서비스 | Method | URL |
|--------|--------|-----|
| 승인/결제 | GET | `{{base_url}}/api/approval/{endpoint}` |
| 정산 | GET | `{{base_url}}/api/settlement/{endpoint}` |
| 매입 청구 | GET | `{{base_url}}/api/billing/{endpoint}` |

---

### 토큰 만료 시 (1시간)

`401 Unauthorized` 응답이 오면 Step 2의 토큰 발급 요청을 다시 보냅니다.
환경변수 `access_token`이 새 토큰으로 자동 갱신되며, 이후 요청은 정상 처리됩니다.

---

## 프로젝트 구조

```
wooricard-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/wooricard/
│   │   │   ├── WooricardGatewayApplication.java
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java       # POST /auth/token
│   │   │   │   └── TokenRequest.java         # 요청 DTO
│   │   │   ├── filter/
│   │   │   │   └── JwtAuthGlobalFilter.java  # 전역 JWT 검증 필터
│   │   │   ├── security/
│   │   │   │   └── SecurityConfig.java       # Spring Security 설정
│   │   │   └── util/
│   │   │       └── JwtUtil.java              # JWT 생성/검증 유틸
│   │   └── resources/
│   │       └── application.yaml             # 운영/통합용 (Eureka + JWT 설정)
│   └── test/
│       ├── java/com/wooricard/
│       │   └── WooricardGatewayApplicationTests.java
│       └── resources/
│           └── application.yaml             # 테스트용 (Eureka 없이 단독 실행)
├── build.gradle
└── README.md
```

---

## 상태 확인

서버를 실행한 후 아래 주소에서 Eureka 등록 여부를 확인하세요.

> ⚠️ **Eureka 서버 IP 주의:** 와이파이 환경이 바뀌면 `application.yaml`의 `eureka.client.service-url.defaultZone`과 `eureka.instance.ip-address`를 현재 IP로 수정해야 합니다.

- **Eureka 대시보드:** http://192.168.1.80:8761
- **Gateway 주소:** http://192.168.1.249:8080

### Eureka 등록 확인 대상

| 서비스명 | Eureka 등록 이름 |
|---------|----------------|
| wooricard-gateway | `WOORICARD-GATEWAY` |
| wooricard-approval-service | `WOORICARD-APPROVAL-SERVICE` |
| wooricard-settlement-service | `WOORICARD-SETTLEMENT-SERVICE` |
| wooricard-billing-service | `WOORICARD-BILLING-SERVICE` |

---

## 관련 서비스

| 서비스명 | 포트 | 설명 |
|---------|------|------|
| wooricard-gateway | 8080 | API Gateway (현재 서비스) |
| wooricard-approval-service | 8081 | 승인/결제 처리 |
| wooricard-settlement-service | 8082 | 정산 처리 |
| wooricard-billing-service | 8083 | 매입 청구 처리 |
| wooricard-eureka | 8761 | 서비스 디스커버리 |
| wooricard-config | 8888 | 설정 서버 |
