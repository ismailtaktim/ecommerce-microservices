-- =====================================================
-- PAYMENT SERVICE DATABASE SCHEMA
-- =====================================================

-- Ödeme durumları için enum
CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);

-- Ödeme yöntemleri için enum
CREATE TYPE payment_method AS ENUM (
    'CREDIT_CARD',
    'DEBIT_CARD',
    'BANK_TRANSFER'
);

-- Başarısızlık nedenleri için enum
CREATE TYPE failure_reason AS ENUM (
    'INSUFFICIENT_FUNDS',
    'CARD_DECLINED',
    'INVALID_CARD',
    'EXPIRED_CARD',
    'TIMEOUT',
    'TECHNICAL_ERROR',
    'FRAUD_SUSPECTED'
);

-- =====================================================
-- PAYMENTS Tablosu
-- =====================================================
CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          order_id UUID NOT NULL UNIQUE,
                          customer_id UUID NOT NULL,
                          amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
                          currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                          method payment_method NOT NULL,
                          status payment_status NOT NULL DEFAULT 'PENDING',
                          transaction_ref VARCHAR(100),
                          failure_reason failure_reason,
                          failure_message TEXT,
                          card_last_four VARCHAR(4),
                          card_brand VARCHAR(20),
                          processed_at TIMESTAMP WITH TIME ZONE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          version BIGINT NOT NULL DEFAULT 0
);

-- =====================================================
-- REFUNDS Tablosu
-- =====================================================
CREATE TYPE refund_status AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED'
);

CREATE TABLE refunds (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         payment_id UUID NOT NULL REFERENCES payments(id),
                         amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
                         status refund_status NOT NULL DEFAULT 'PENDING',
                         reason TEXT,
                         transaction_ref VARCHAR(100),
                         processed_at TIMESTAMP WITH TIME ZONE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PAYMENT_ATTEMPTS Tablosu
-- =====================================================
CREATE TABLE payment_attempts (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
                                  status payment_status NOT NULL,
                                  failure_reason failure_reason,
                                  failure_message TEXT,
                                  response_code VARCHAR(20),
                                  response_message TEXT,
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- IDEMPOTENCY_KEYS Tablosu
-- =====================================================
CREATE TABLE idempotency_keys (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  idempotency_key VARCHAR(255) NOT NULL UNIQUE,
                                  payment_id UUID REFERENCES payments(id),
                                  request_hash VARCHAR(64) NOT NULL,
                                  response JSONB,
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- =====================================================
-- OUTBOX_EVENTS Tablosu
-- =====================================================
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               payload JSONB NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP WITH TIME ZONE,
                               published BOOLEAN NOT NULL DEFAULT FALSE
);

-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_payment_attempts_payment ON payment_attempts(payment_id);
CREATE INDEX idx_idempotency_keys_key ON idempotency_keys(idempotency_key);
CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys(expires_at);
CREATE INDEX idx_outbox_unpublished ON outbox_events(published, created_at) WHERE published = FALSE;

-- =====================================================
-- TRIGGERS
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at
    BEFORE UPDATE ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();