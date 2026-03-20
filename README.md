# wooricard-gateway

Spring Cloud Gateway 기반 API Gateway 서비스

---

## 프로젝트 개요

카드 처리 MSA 시스템의 단일 진입점(API Gateway) 역할을 담당합니다.
클라이언트의 모든 요청을 받아 각 마이크로서비스로 라우팅합니다.

---

## 시스템 아키텍처

```
Client
  │
  ▼
API Gateway (8080)  ──── Eureka Server (8761)
  │                 ──── Config Server (8888)
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

---

## 라우팅 규칙

| 서비스 | 경로 패턴 | 연결 방식 (Eureka) | 연결 방식 (로컬) |
|--------|----------|--------------------|-----------------|
| 승인/결제 서비스 (wooricard-approval-service) | `/api/approval/**` | `lb://wooricard-approval-service` | `http://localhost:8081` |
| 정산 서비스 (wooricard-settlement-service) | `/api/settlement/**` | `lb://wooricard-settlement-service` | `http://localhost:8082` |
| 매입 청구 서비스 (wooricard-billing-service) | `/api/billing/**` | `lb://wooricard-billing-service` | `http://localhost:8083` |

---

## 실행 방법

### 실행 순서 (운영/통합 환경)

```
1. wooricard-eureka     (port: 8761)
2. wooricard-config     (port: 8888)
3. wooricard-gateway    (port: 8080)  ← 이 서비스
4. 각 마이크로서비스 실행
```

### 실행 방법

```bash
./gradlew bootRun
```

IntelliJ: `Active profiles` 비워두고 실행

---

## API 테스트 (APIdog)

### Base URL 설정

```
http://localhost:8080
```

### 엔드포인트 예시

| 서비스 | 요청 URL |
|--------|---------|
| 승인/결제 | `http://localhost:8080/api/approval/{endpoint}` |
| 정산 | `http://localhost:8080/api/settlement/{endpoint}` |
| 매입 청구 | `http://localhost:8080/api/billing/{endpoint}` |

### 라우팅 정상 확인 방법

동일한 요청을 게이트웨이 경유와 서비스 직접 호출로 비교합니다.

```
# 게이트웨이 경유
GET http://localhost:8080/api/approval/{endpoint}

# 서비스 직접 호출
GET http://localhost:8081/api/approval/{endpoint}
```

두 응답이 동일하면 라우팅이 정상적으로 동작하는 것입니다.

---

## 프로젝트 구조

```
wooricard-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/wooricard/
│   │   │   └── WooricardGatewayApplication.java
│   │   └── resources/
│   │       └── application.yaml          # 운영/통합용 (Eureka 연동)
│   └── test/
│       ├── java/com/wooricard/
│       │   └── WooricardGatewayApplicationTests.java
│       └── resources/
│           └── application.yaml          # 테스트용 (Eureka 비활성화)
├── build.gradle
└── README.md
```

---

## 상태 확인 방법

서버를 실행한 후 아래 주소에 접속하여 본인의 서비스 이름이 **Instances currently registered with Eureka** 목록에 뜨는지 확인하세요.

> ⚠️ **Eureka 서버 IP 주의:** 와이파이 환경이 바뀌면 `application.yaml`의 `eureka.client.service-url.defaultZone` 주소를 현재 유레카 서버 IP로 수정해야 합니다.

- **Eureka 대시보드:** http://192.168.1.80:8761 (현재 와이파이 기준 — 변경 시 수정 필요)
- **Gateway 상태:** http://192.168.0.26:8080

### Eureka 등록 확인 대상

| 서비스명 | Eureka 등록 이름 |
|---------|----------------|
| wooricard-gateway | `WOORICARD-GATEWAY` |
| wooricard-approval-service | `WOORICARD-APPROVAL-SERVICE` |
| wooricard-settlement-service | `WOORICARD-SETTLEMENT-SERVICE` |
| wooricard-billing-service | `WOORICARD-BILLING-SERVICE` |

---

## 관련 서비스

| 서비스명 | 포트 | 설명                   |
|---------|------|----------------------|
| wooricard-gateway | 8080 | API Gateway (현재 서비스) |
| wooricard-approval-service | 8081 | 승인/결제 처리             |
| wooricard-settlement-service | 8082 | 정산 처리                |
| wooricard-billing-service | 8083 | 매입 청구 처리             |
| wooricard-eureka | 8761 | 서비스 디스커버리            |
| wooricard-config | 8888 | 설정 서버                |
