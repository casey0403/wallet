# Wallet Service

동일 월렛에 대한 다수의 동시 출금 요청에서도 잔액 무결성을 보장하는 Spring Boot 기반 월렛 API입니다.

## 기술 스택

- Kotlin 1.9
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- MySQL 8.4
- Redis 7.4
- Docker Compose
- Testcontainers

## 실행 방법

### 1. 인프라 실행

```bash
docker compose up -d mysql redis
```

MySQL과 Redis가 실행됩니다.

- MySQL: `localhost:3306`
- Redis: `localhost:6379`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## DB 세팅

MySQL 컨테이너가 처음 생성될 때 아래 DDL이 자동 실행됩니다.

```text
docker/mysql/init/001_create_wallet_schema.sql
```

Docker Compose가 이 경로를 MySQL 초기화 디렉터리에 마운트합니다.

```yaml
./docker/mysql/init:/docker-entrypoint-initdb.d:ro
```

이미 생성된 DB volume이 있으면 init SQL은 다시 실행되지 않습니다. 스키마를 처음부터 다시 만들려면 다음 명령을 실행합니다.

```bash
docker compose down -v
docker compose up -d mysql redis
```

### 초기 데이터

DDL에 테스트용 유저와 월렛이 포함되어 있습니다.

```sql
INSERT INTO users (id, email, name)
VALUES ('sample-user', 'sample@example.com', 'Sample User')
ON DUPLICATE KEY UPDATE email = VALUES(email), name = VALUES(name);

INSERT INTO wallets (id, user_id, balance, currency, version, wallet_status)
VALUES ('sample-wallet', 'sample-user', 100000.0000, 'KRW', 0, 'ACTIVE')
ON DUPLICATE KEY UPDATE balance = VALUES(balance), currency = VALUES(currency), wallet_status = VALUES(wallet_status);
```

## API

API 문서는 Spring REST Docs 기반 테스트로 생성합니다. 비즈니스 컨트롤러에는 OpenAPI/Swagger annotation을 두지 않고, `src/test`의 문서화 테스트가 실제 API를 호출하며 request/response snippet과 OpenAPI 3 스펙을 생성합니다.

문서 생성:

```bash
./gradlew test --tests com.wannabe.wallet.docs.WalletApiDocumentationTest --rerun-tasks openapi3
```

생성 결과:

```text
build/generated-snippets/
build/api-spec/openapi3.yaml
```

애플리케이션 실행 후 Swagger UI로 확인:

```bash
./gradlew bootRun
```

브라우저에서 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)로 접속하면 테스트로 생성된 OpenAPI 문서를 확인할 수 있습니다. Swagger UI는 런타임 reflection으로 API 문서를 만들지 않고, `build/api-spec/openapi3.yaml`을 `/openapi/openapi3.yaml`로 서빙해서 표시합니다.

아래 예시는 로컬에서 빠르게 호출해보기 위한 quick reference입니다.

### 출금

```http
POST /api/v1/wallets/{walletId}/withdrawals
```

요청:

```json
{
  "amount": 10000,
  "transactionId": "TXN-001",
  "currency": "KRW"
}
```

성공 응답 `200 OK`:

```json
{
  "transactionId": "TXN-001",
  "walletId": "sample-wallet",
  "type": "WITHDRAWAL",
  "status": "SUCCESS",
  "withdrawalAmount": 10000,
  "currency": "KRW",
  "balance": 90000.0000,
  "version": 1,
  "withdrawalDate": "2026-05-21T01:30:36.117000"
}
```

실패 응답은 `code`, `message`, `data` 형식을 사용합니다.

잔액 부족 `409 Conflict`:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient wallet balance",
  "data": {
    "transactionId": "TXN-002",
    "walletId": "sample-wallet",
    "type": "WITHDRAWAL",
    "status": "FAILED",
    "withdrawalAmount": 1000000,
    "currency": "KRW",
    "balance": 90000.0000,
    "version": 1,
    "withdrawalDate": "2026-05-21T01:30:36.117000",
    "failureCode": "INSUFFICIENT_BALANCE",
    "failureMessage": "Insufficient wallet balance"
  }
}
```

### 거래내역 조회

```http
GET /api/v1/wallets/{walletId}/transactions
```

응답:

```json
{
  "transactions": [
    {
      "transactionId": "TXN-001",
      "walletId": "sample-wallet",
      "type": "WITHDRAWAL",
      "status": "SUCCESS",
      "withdrawalAmount": 10000,
      "currency": "KRW",
      "balance": 90000.0000,
      "version": 1,
      "withdrawalDate": "2026-05-21T01:30:36.117000"
    }
  ]
}
```

### curl 예시

```bash
curl -X POST http://localhost:8080/api/v1/wallets/sample-wallet/withdrawals \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "transactionId": "TXN-001",
    "currency": "KRW"
  }'
```

```bash
curl http://localhost:8080/api/v1/wallets/sample-wallet/transactions
```

## 설계 결정

ERD 문서는 [docs/wallet-erd.html](docs/wallet-erd.html)에 정리했습니다.

### 패키지 구조

헥사고날 아키텍처와 DDD 계층 구조를 조합했습니다.

```text
presentation
  controller
  model
    request
    response
application
  wallet
    assembler
    command
    dto
    query
domain
  wallet
    enums
    error
    exception
    model
    port
    service
    vo
infrastructure
  adapter
  jpa
    entity
```

- `presentation`: REST API 요청/응답 모델과 컨트롤러
- `application`: 유스케이스 조합, command/query service, assembler, application DTO
- `domain`: 월렛 도메인 모델, VO, enum, error code, exception, 도메인 서비스, port
- `infrastructure`: JPA repository, `*JPAEntity`, Redis lock adapter, persistence adapter

의존성 방향은 `presentation -> application -> domain <- infrastructure`를 따릅니다. application은 Redis나 JPA 구현체를 직접 참조하지 않고 `WalletLock`, `WalletCommandStore`, `WalletQueryStore` 포트에 의존합니다.

### 동시성 제어

출금은 다음 순서로 처리합니다.

1. Redis 분산락을 `wallet:withdraw:{walletId}` 단위로 획득합니다.
2. DB 트랜잭션 안에서 멱등성 요청을 확인하거나 생성합니다.
3. 월렛의 현재 `balance`, `version`, `wallet_status`를 기준으로 조건부 atomic update를 수행합니다.
4. 성공 또는 실패 거래내역을 저장합니다.
5. 멱등성 응답 snapshot을 저장합니다.
6. Redis lock을 Lua script로 안전하게 해제합니다.

출금 API의 주요 흐름은 다음과 같습니다.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as WalletController
    participant CommandService as WalletCommandService
    participant Lock as "WalletLock(Redis)"
    participant Processor as WalletWithdrawalProcessor
    participant Store as WalletCommandStore
    participant DB as MySQL

    Client->>Controller: POST /api/v1/wallets/{walletId}/withdrawals
    Controller->>CommandService: withdraw(walletId, amount, currency, transactionId)
    CommandService->>CommandService: Money 생성 및 출금 금액 검증
    CommandService->>Lock: wallet 단위 Redis lock 획득

    alt lock 획득 실패
        Lock-->>CommandService: WALLET_BUSY
        CommandService-->>Controller: 실패
        Controller-->>Client: 409 ErrorResponse
    else lock 획득 성공
        Lock->>Processor: 출금 처리 실행
        Processor->>Store: wallet 조회
        Store->>DB: SELECT wallet
        DB-->>Store: wallet
        Store-->>Processor: wallet

        Processor->>Store: idempotency request 조회
        Store->>DB: SELECT by walletId + transactionId
        DB-->>Store: 기존 요청 또는 없음

        alt 동일 transactionId의 완료 요청 존재
            Store-->>Processor: responseSnapshot
            Processor-->>Lock: 이전 응답 반환
            Lock-->>CommandService: 이전 응답
            CommandService-->>Controller: 이전 응답
            Controller-->>Client: 동일 응답
        else 최초 요청
            Processor->>Store: idempotency request PROCESSING 저장
            Store->>DB: INSERT idempotency_requests

            Processor->>Store: 조건부 atomic update
            Store->>DB: UPDATE wallets SET balance = balance - amount, version = version + 1 WHERE id/version/balance/currency/status 조건

            alt update 성공
                DB-->>Store: updatedRows = 1
                Processor->>Store: 성공 거래내역 저장
                Store->>DB: INSERT wallet_transactions SUCCESS
                Processor->>Store: responseSnapshot 저장
                Store->>DB: UPDATE idempotency_requests COMPLETED
                Processor-->>Lock: 성공 응답
                Lock-->>CommandService: 성공 응답
                CommandService-->>Controller: 성공 응답
                Controller-->>Client: 200 TransactionResponse
            else update 실패
                DB-->>Store: updatedRows = 0
                Processor->>Store: 최신 wallet 재조회
                Store->>DB: SELECT wallet
                Processor->>Store: 실패 거래내역 저장
                Store->>DB: INSERT wallet_transactions FAILED
                Processor->>Store: responseSnapshot 저장
                Store->>DB: UPDATE idempotency_requests COMPLETED
                Processor-->>Lock: 실패 응답
                Lock-->>CommandService: 실패 응답
                CommandService-->>Controller: 실패 응답
                Controller-->>Client: 409 ErrorResponse
            end
        end

        Lock->>Lock: Lua script로 lock token 검증 후 해제
    end
```

DB 최종 방어선은 다음 조건부 update입니다.

```sql
UPDATE wallets
   SET balance = balance - :amount,
       version = version + 1
 WHERE id = :walletId
   AND version = :version
   AND balance >= :amount
   AND currency = :currency
   AND wallet_status = 'ACTIVE';
```

Redis lock은 같은 wallet의 요청을 짧게 직렬화해서 DB 충돌을 줄입니다. 그러나 Redis lock만으로 무결성을 보장하지 않고, DB의 `version`과 `balance >= amount` 조건을 최종 방어선으로 둡니다. 따라서 lock TTL 만료, 애플리케이션 지연, 재시도 상황에서도 overdraft와 lost update를 방지할 수 있습니다.

### 트레이드오프

- Redis lock을 사용하면 같은 wallet에 대한 출금은 순차 처리되어 처리량이 제한됩니다.
- 대신 wallet 단위 lock이라 서로 다른 wallet의 출금은 병렬 처리할 수 있습니다.
- `SELECT FOR UPDATE`를 오래 유지하지 않고 조건부 `UPDATE`를 사용하여 DB row lock 점유 시간을 줄였습니다.
- Redis 장애 시 출금 API가 영향을 받을 수 있습니다. 운영 환경에서는 Redis HA 구성, lock 획득 실패 재시도 정책, timeout/metrics/alerting이 필요합니다.

### 멱등성

`transactionId`를 멱등성 키로 사용합니다.

- 동일 `walletId`, `transactionId`, 요청 파라미터가 재요청되면 이전 응답을 반환합니다.
- 동일 `transactionId`를 다른 금액/통화 요청에 재사용하면 `IDEMPOTENCY_KEY_CONFLICT`를 반환합니다.
- 요청 파라미터는 SHA-256 hash로 저장해 중복 키의 요청 내용 불일치를 검출합니다.

멱등성 응답 snapshot은 `LONGTEXT`로 저장합니다. MySQL `JSON` 타입은 숫자 값을 정규화할 수 있어 `10000.0000`이 `10000.0`처럼 저장될 수 있습니다. 이 경우 재요청 시 byte-level 응답 동일성이 깨질 수 있으므로, 직렬화된 원본 응답 문자열을 그대로 보존하기 위해 `LONGTEXT`를 선택했습니다.

## 테스트

### 전체 테스트

```bash
./gradlew test
```

### 동시성 통합 테스트만 실행

```bash
./gradlew test --tests "com.wannabe.wallet.integration.WalletConcurrencyIntegrationTest" --rerun-tasks
```

테스트는 Testcontainers로 MySQL 8.4와 Redis 7.4를 실행합니다.

## 동시성 테스트 결과

실행 명령:

```bash
./gradlew test --tests com.wannabe.wallet.integration.WalletConcurrencyIntegrationTest --rerun-tasks
```

실행 결과:

```text
BUILD SUCCESSFUL in 16s
4 tests completed, 4 passed
```

### 동시성 제어 적용 전

테스트 `unsafe read-modify-write loses balance integrity`는 프로덕션 API를 사용하지 않고, 테스트 코드에서 의도적으로 unsafe read-modify-write를 수행합니다.

결과:

```text
[BEFORE] threads=100 amount=10000.0000 successful=100 requestedTotal=1000000.0000 finalBalance=490000.0000 version=100 result=LOST_UPDATE
```

해석:

- 100개 요청이 모두 성공 가능하다고 판단했습니다.
- 총 요청 금액은 `1000000.0000`으로 초기 잔액 `500000.0000`을 초과합니다.
- 최종 잔액은 `490000.0000`으로 남아 lost update가 발생했습니다.
- 동시성 제어 없이 read-modify-write를 수행하면 잔액 무결성이 깨질 수 있음을 보여줍니다.

### 동시성 제어 적용 후

테스트 `same wallet withdrawals are processed sequentially without overdraft`는 실제 출금 API를 HTTP로 동시에 호출합니다.

결과:

```text
[AFTER] threads=100 amount=10000.0000 success=50 failed=50 withdrawnTotal=500000.0000 finalBalance=0.0000 version=50 result=OK
```

해석:

- 초기 잔액 `500000.0000`에서 `10000.0000` 출금은 정확히 50번만 성공했습니다.
- 나머지 50번은 잔액 부족으로 실패했습니다.
- 최종 잔액은 `0.0000`이며 음수가 되지 않았습니다.
- 성공 출금 총액은 초기 잔액을 초과하지 않았습니다.

추가 자료는 [docs/concurrency-test.md](docs/concurrency-test.md)에 정리했습니다.

## 로컬 확인 명령

MySQL row 확인:

```bash
docker compose exec mysql mysql -uwallet -pwallet wallet \
  -e "SELECT * FROM wallets; SELECT * FROM wallet_transactions; SELECT * FROM idempotency_requests;"
```

Redis key 확인:

```bash
docker compose exec redis redis-cli KEYS '*'
```

정상 처리 시 Redis lock key는 요청 종료와 함께 삭제되므로 대부분 보이지 않는 것이 정상입니다.
