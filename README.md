# Backend

Spring Boot 기반의 백엔드 서버로, AI 예측 서버(FastAPI)와 연동하여 이미지 파일을 분석하고 예측 결과를 반환합니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Reactive HTTP Client | Spring WebFlux (WebClient) |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| Lombok | 보일러플레이트 코드 제거 |
| Build Tool | Gradle |

## 프로젝트 구조

```
src/main/java/com/demo/backend/
├── BackendApplication.java          # 애플리케이션 진입점
└── AI/
    ├── config/
    │   └── WebClientConfig.java     # WebClient Bean 설정
    ├── controller/
    │   └── AiController.java        # AI 예측 API 엔드포인트
    ├── dto/
    │   └── response/
    │       └── AIPredictResponse.java  # AI 예측 응답 DTO
    └── service/
        └── AIService.java           # FastAPI 호출 비즈니스 로직
```

## API 명세

### AI 예측

| 항목 | 내용 |
|------|------|
| Method | `POST` |
| URL | `/api/ai/predict` |
| Content-Type | `multipart/form-data` |
| Request | `file` (MultipartFile) |
| Response | `{ "prediction": int }` |

## 실행 방법

### 사전 요구사항

- Java 17 이상
- FastAPI AI 서버가 `http://localhost:8000`에서 실행 중이어야 합니다.

### 실행

```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## Swagger UI

서버 실행 후 아래 주소에서 API 문서를 확인하고 직접 호출할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

### 파일 업로드 API 호출 방법

1. Swagger UI 접속
2. `AI` 섹션의 `POST /api/ai/predict` 클릭
3. **Try it out** 버튼 클릭
4. `file` 항목의 **Choose File**로 이미지 파일 선택
5. **Execute** 버튼 클릭

## 아키텍처

```
클라이언트
    │
    ▼
Spring Boot (8080)
    │  POST /api/ai/predict
    │  multipart/form-data
    ▼
FastAPI AI Server (8000)
    │  POST /predict
    ▼
예측 결과 반환
```

Spring Boot 서버는 클라이언트로부터 파일을 수신하고, WebClient를 통해 FastAPI 서버의 `/predict` 엔드포인트에 파일을 전달한 후 예측 결과를 클라이언트에 반환합니다.

## 딥러닝 모델 연동을 위해 학습하면 좋은 것들

### 1. HTTP 통신 — WebClient / RestClient

백엔드에서 AI 서버를 호출하는 핵심 수단입니다.

| 개념 | 설명 |
|------|------|
| `WebClient` | Spring WebFlux 기반의 비동기 HTTP 클라이언트. 현재 프로젝트에서 사용 중 |
| `RestClient` | Spring 6.1+에서 도입된 동기식 HTTP 클라이언트. WebClient보다 간결 |
| `block()` | 비동기 호출을 동기처럼 기다리게 하는 메서드. 성능 이슈가 생길 수 있어 주의 필요 |

#### WebClient 개념

WebClient는 기존의 `RestTemplate`을 대체하는 HTTP 클라이언트입니다.
**Non-blocking(비동기)** 방식으로 동작하기 때문에, 요청을 보내고 응답을 기다리는 동안 스레드를 점유하지 않고 다른 작업을 처리할 수 있습니다.

WebClient의 응답은 `Mono<T>` (단일 값) 또는 `Flux<T>` (스트림) 타입으로 반환됩니다.

```
요청 전송 → 응답 대기 중 스레드 반환 → 응답 도착 시 콜백 실행
```

#### AIService에서의 사용 흐름

```java
// 1. WebClientConfig에서 Bean으로 등록된 WebClient를 생성자 주입으로 받아옴
private final WebClient webClient;

// 2. 클라이언트로부터 받은 MultipartFile을 WebClient로 전송 가능한 형태로 변환
ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
    @Override
    public String getFilename() {
        return file.getOriginalFilename(); // 파일명을 유지해야 FastAPI에서 인식 가능
    }
};

// 3. multipart/form-data body 구성
MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
formData.add("file", resource); // key: "file" → FastAPI의 파라미터 이름과 일치해야 함

// 4. WebClient로 FastAPI 호출
return webClient.post()                              // POST 요청
        .uri("http://localhost:8000/predict")        // 대상 URL
        .contentType(MediaType.MULTIPART_FORM_DATA)  // Content-Type 헤더 설정
        .bodyValue(formData)                         // 요청 body
        .retrieve()                                  // 응답 수신 시작
        .bodyToMono(AIPredictResponse.class)         // 응답 JSON → 객체 변환 (비동기)
        .block();                                    // 비동기를 동기처럼 응답 대기
```

각 메서드 체인의 역할:

| 메서드 | 역할 |
|--------|------|
| `.post()` | HTTP 메서드 지정 |
| `.uri(...)` | 요청 보낼 URL 지정 |
| `.contentType(...)` | 요청의 Content-Type 헤더 지정 |
| `.bodyValue(...)` | 요청 body 데이터 설정 |
| `.retrieve()` | 응답을 받아오기 시작 (오류 코드 자동 처리 포함) |
| `.bodyToMono(...)` | 응답 body를 지정한 클래스로 역직렬화 |
| `.block()` | `Mono`를 동기적으로 기다려 결과값 반환 |

> `.block()`을 사용하면 동기처럼 동작하지만 WebFlux의 비동기 이점이 사라집니다.
> 추론 시간이 긴 모델을 다룰 경우 `Mono<AIPredictResponse>`를 그대로 반환하는 비동기 방식으로 개선하는 것을 권장합니다.

---

### 2. multipart/form-data

파일을 서버 간에 전송할 때 사용하는 인코딩 방식입니다.

- `MultipartFile` — Spring에서 업로드된 파일을 다루는 인터페이스
- `ByteArrayResource` — `MultipartFile`을 WebClient로 전송하기 위해 변환하는 클래스
- `LinkedMultiValueMap` — multipart body를 구성할 때 사용하는 Map

---

### 3. AI 모델 서빙 프레임워크

딥러닝 모델을 API로 제공하는 방법은 여러 가지가 있습니다.

| 프레임워크 | 특징 |
|------------|------|
| **FastAPI** | Python 기반, 가볍고 빠름. 현재 프로젝트에서 사용 |
| **TorchServe** | PyTorch 공식 서빙 도구. 모델 버전 관리 지원 |
| **TensorFlow Serving** | TensorFlow 공식 서빙 도구. gRPC/REST 지원 |
| **Triton Inference Server** | NVIDIA 제공, 고성능 GPU 서빙에 적합 |

---

### 4. 비동기 처리 — 메시지 큐

추론 시간이 긴 모델(수 초 이상)은 HTTP 요청을 바로 기다리지 않고 큐에 작업을 넣는 방식이 적합합니다.

| 기술 | 설명 |
|------|------|
| **Kafka** | 대용량 처리에 강한 분산 메시지 스트리밍 플랫폼 |
| **RabbitMQ** | 구현이 간단하고 소규모 서비스에 적합한 메시지 브로커 |

흐름 예시:
```
클라이언트 → Spring Boot → Kafka → AI 서버 → 결과 저장 → 클라이언트 폴링
```
