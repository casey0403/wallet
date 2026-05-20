# Wallet API

## Response Rule

Successful responses return only the requested data.

```json
{
  "transactionId": "TXN-001",
  "walletId": "sample-wallet"
}
```

Error responses use the same envelope. `data` is `null` for request/system errors, and may contain a transaction snapshot for business failures that are recorded in the ledger.

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient wallet balance",
  "data": {}
}
```

## Withdraw

`POST /api/v1/wallets/{walletId}/withdrawals`

### Request

```json
{
  "amount": 10000,
  "transactionId": "TXN-001",
  "currency": "KRW"
}
```

### 200 OK

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
  "withdrawalDate": "2026-05-21T00:38:35.658507"
}
```

### 409 Conflict - Insufficient Balance

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
    "withdrawalDate": "2026-05-21T00:38:35.658507",
    "failureCode": "INSUFFICIENT_BALANCE",
    "failureMessage": "Insufficient wallet balance"
  }
}
```

### 409 Conflict - Idempotency Key Conflict

Returned when the same `transactionId` is reused with different request parameters.

```json
{
  "code": "IDEMPOTENCY_KEY_CONFLICT",
  "message": "Transaction id was reused with different request parameters",
  "data": null
}
```

## Transactions

`GET /api/v1/wallets/{walletId}/transactions`

### 200 OK

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
      "withdrawalDate": "2026-05-21T00:38:35.658507"
    }
  ]
}
```
