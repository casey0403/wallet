# Concurrency Test Evidence

## Goal

Verify that concurrent withdrawal requests for the same wallet do not create overdrafts or lost updates.

## Scenario

- Wallet ID: `concurrency-wallet`
- Initial balance: `500000.0000`
- Thread count: `100`
- Withdrawal amount per request: `10000.0000`
- Total requested amount: `1000000.0000`

## Run

```bash
docker compose up -d mysql redis
./gradlew test --tests "com.wannabe.wallet.integration.WalletConcurrencyIntegrationTest" --rerun-tasks
```

If the local database was already initialized with an old schema:

```bash
docker compose down -v
docker compose up -d mysql redis
./gradlew test --tests "com.wannabe.wallet.integration.WalletConcurrencyIntegrationTest" --rerun-tasks
```

## Before Concurrency Control

The test `unsafe read-modify-write loses balance integrity` intentionally performs an unsafe read-modify-write update in test code only. It does not use the production API.

Expected summary:

```text
[BEFORE] threads=100 amount=10000.0000 successful=100 requestedTotal=1000000.0000 finalBalance=490000.0000 version=100 result=LOST_UPDATE
```

Interpretation:

- All 100 requests believed they succeeded.
- Requested total was `1000000.0000`, which exceeds the initial balance.
- Final balance became `490000.0000` because concurrent writers overwrote each other.
- This proves lost update can occur without concurrency control.

## After Concurrency Control

The test `same wallet withdrawals are processed sequentially without overdraft` sends concurrent HTTP requests to the production withdrawal API.

Expected summary:

```text
[AFTER] threads=100 amount=10000.0000 success=50 failed=50 withdrawnTotal=500000.0000 finalBalance=0.0000 version=50 result=OK
```

Interpretation:

- Only 50 withdrawals succeeded because the initial balance supports exactly 50 withdrawals.
- 50 withdrawals failed with insufficient balance.
- Final balance remained `0.0000`, never negative.
- Successful withdrawal total did not exceed the initial balance.

## Implemented Technique

- Redis distributed lock: serializes withdrawal handling per `walletId`.
- DB atomic update: `UPDATE wallets ... WHERE id = ? AND version = ? AND balance >= ? AND wallet_status = 'ACTIVE'`.
- Version column: prevents lost update if a stale request attempts to update the wallet.
- Idempotency table: stores the serialized response snapshot and returns the same response for duplicate `transactionId` requests without changing balance again.
