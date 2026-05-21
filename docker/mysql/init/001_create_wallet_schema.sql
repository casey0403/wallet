CREATE TABLE users (
    id VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallets (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NULL,
    balance DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL,
    wallet_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallets_user_id_currency (user_id, currency),
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_wallets_version_non_negative CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE idempotency_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    http_status INT NULL,
    response_snapshot LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_requests_wallet_key (wallet_id, idempotency_key),
    KEY idx_idempotency_requests_expires_at (expires_at),
    CONSTRAINT fk_idempotency_requests_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_idempotency_requests_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id VARCHAR(128) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL,
    idempotency_request_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    wallet_version_before BIGINT NOT NULL,
    wallet_version_after BIGINT NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_transactions_wallet_transaction_id (wallet_id, transaction_id),
    UNIQUE KEY uk_wallet_transactions_idempotency_request_id (idempotency_request_id),
    KEY idx_wallet_transactions_wallet_id_processed_at (wallet_id, processed_at DESC, id DESC),
    CONSTRAINT fk_wallet_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_wallet_transactions_idempotency_request
        FOREIGN KEY (idempotency_request_id) REFERENCES idempotency_requests (id),
    CONSTRAINT chk_wallet_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_wallet_transactions_balance_after_non_negative CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO users (id, email, name)
VALUES ('sample-user', 'sample@example.com', 'Sample User')
ON DUPLICATE KEY UPDATE email = VALUES(email), name = VALUES(name);

INSERT INTO wallets (id, user_id, balance, currency, version, wallet_status)
VALUES ('sample-wallet', 'sample-user', 100000.0000, 'KRW', 0, 'ACTIVE')
ON DUPLICATE KEY UPDATE balance = VALUES(balance), currency = VALUES(currency), wallet_status = VALUES(wallet_status);
