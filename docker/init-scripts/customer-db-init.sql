-- =====================================================
-- CUSTOMER SERVICE DATABASE SCHEMA
-- =====================================================

-- Müşteri durumları için enum
CREATE TYPE customer_status AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED'
);

-- Kullanıcı rolleri için enum
CREATE TYPE customer_role AS ENUM (
    'CUSTOMER',
    'ADMIN'
);

-- =====================================================
-- CUSTOMERS Tablosu
-- =====================================================
CREATE TABLE customers (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           email VARCHAR(255) NOT NULL UNIQUE,
                           password_hash VARCHAR(255) NOT NULL,
                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,
                           phone VARCHAR(20),
                           status customer_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
                           role customer_role NOT NULL DEFAULT 'CUSTOMER',
                           email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                           email_verification_token VARCHAR(255),
                           email_verification_expires_at TIMESTAMP WITH TIME ZONE,
                           password_reset_token VARCHAR(255),
                           password_reset_expires_at TIMESTAMP WITH TIME ZONE,
                           last_login_at TIMESTAMP WITH TIME ZONE,
                           failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                           locked_until TIMESTAMP WITH TIME ZONE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           version BIGINT NOT NULL DEFAULT 0
);

-- =====================================================
-- CUSTOMER_ADDRESSES Tablosu
-- =====================================================
CREATE TABLE customer_addresses (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                                    title VARCHAR(100) NOT NULL,
                                    recipient_name VARCHAR(200) NOT NULL,
                                    recipient_phone VARCHAR(20) NOT NULL,
                                    address_line1 VARCHAR(500) NOT NULL,
                                    address_line2 VARCHAR(500),
                                    district VARCHAR(100) NOT NULL,
                                    city VARCHAR(100) NOT NULL,
                                    postal_code VARCHAR(20),
                                    country VARCHAR(100) NOT NULL DEFAULT 'Türkiye',
                                    is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- REFRESH_TOKENS Tablosu
-- =====================================================
CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                device_info VARCHAR(500),
                                ip_address VARCHAR(50),
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                revoked_at TIMESTAMP WITH TIME ZONE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_addresses_customer ON customer_addresses(customer_id);
CREATE INDEX idx_addresses_customer_active ON customer_addresses(customer_id) WHERE is_active = TRUE;
CREATE INDEX idx_refresh_tokens_customer ON refresh_tokens(customer_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token_hash);

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

CREATE TRIGGER update_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_addresses_updated_at
    BEFORE UPDATE ON customer_addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ÖRNEK VERİ
-- =====================================================
INSERT INTO customers (id, email, password_hash, first_name, last_name, phone, status, role, email_verified)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'admin@ecommerce.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMye.IFphPBtGXFjX3nE1h8oVpgjJyHlPEK',
        'System',
        'Admin',
        '5551234567',
        'ACTIVE',
        'ADMIN',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'test@example.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMye.IFphPBtGXFjX3nE1h8oVpgjJyHlPEK',
        'Test',
        'User',
        '5559876543',
        'ACTIVE',
        'CUSTOMER',
        TRUE
    );

INSERT INTO customer_addresses (customer_id, title, recipient_name, recipient_phone, address_line1, district, city, postal_code, is_default)
VALUES
    (
        '00000000-0000-0000-0000-000000000002',
        'Ev',
        'Test User',
        '5559876543',
        'Atatürk Caddesi No: 123 Daire: 4',
        'Kadıköy',
        'İstanbul',
        '34710',
        TRUE
    );