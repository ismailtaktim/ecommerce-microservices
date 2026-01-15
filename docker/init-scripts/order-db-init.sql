-- =====================================================
-- ORDER SERVICE DATABASE SCHEMA
-- =====================================================

-- Sipariş durumları için enum
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'INVENTORY_RESERVED',
    'PAYMENT_COMPLETED',
    'COMPLETED',
    'CANCELLED',
    'FAILED'
);

-- =====================================================
-- ORDERS Tablosu
-- =====================================================
CREATE TABLE orders (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        order_number VARCHAR(20) NOT NULL UNIQUE,
                        customer_id UUID NOT NULL,
                        customer_email VARCHAR(255) NOT NULL,
                        customer_phone VARCHAR(20),
                        status order_status NOT NULL DEFAULT 'PENDING',

    -- Teslimat adresi (sipariş anında kopyalanır)
                        shipping_recipient_name VARCHAR(200) NOT NULL,
                        shipping_phone VARCHAR(20) NOT NULL,
                        shipping_address_line1 VARCHAR(500) NOT NULL,
                        shipping_address_line2 VARCHAR(500),
                        shipping_district VARCHAR(100) NOT NULL,
                        shipping_city VARCHAR(100) NOT NULL,
                        shipping_postal_code VARCHAR(20),
                        shipping_country VARCHAR(100) NOT NULL DEFAULT 'Türkiye',

    -- Finansal bilgiler
                        subtotal DECIMAL(15, 2) NOT NULL,
                        tax_amount DECIMAL(15, 2) NOT NULL,
                        total_amount DECIMAL(15, 2) NOT NULL,
                        currency VARCHAR(3) NOT NULL DEFAULT 'TRY',

    -- İptal/Hata bilgileri
                        cancellation_reason TEXT,
                        failure_reason TEXT,
                        cancelled_by VARCHAR(20),

    -- Zaman damgaları
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        version BIGINT NOT NULL DEFAULT 0
);

-- =====================================================
-- ORDER_ITEMS Tablosu
-- =====================================================
CREATE TABLE order_items (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id UUID NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             product_sku VARCHAR(100) NOT NULL,
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price DECIMAL(15, 2) NOT NULL,
                             total_price DECIMAL(15, 2) NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ORDER_STATUS_HISTORY Tablosu
-- =====================================================
CREATE TABLE order_status_history (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                      old_status order_status,
                                      new_status order_status NOT NULL,
                                      reason TEXT,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- SAGA_STATES Tablosu (Orchestration için)
-- =====================================================
CREATE TYPE saga_status AS ENUM (
    'STARTED',
    'INVENTORY_PENDING',
    'INVENTORY_RESERVED',
    'PAYMENT_PENDING',
    'PAYMENT_COMPLETED',
    'NOTIFICATION_PENDING',
    'COMPLETED',
    'COMPENSATING',
    'FAILED'
);

CREATE TABLE saga_states (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
                             status saga_status NOT NULL DEFAULT 'STARTED',
                             current_step VARCHAR(50) NOT NULL,
                             payload JSONB,
                             started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             completed_at TIMESTAMP WITH TIME ZONE,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- OUTBOX_EVENTS Tablosu (Transactional Outbox)
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
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_id);
CREATE INDEX idx_saga_states_order ON saga_states(order_id);
CREATE INDEX idx_saga_states_status ON saga_states(status);
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

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_saga_states_updated_at
    BEFORE UPDATE ON saga_states
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Sipariş numarası üretme fonksiyonu
-- =====================================================
CREATE SEQUENCE order_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.order_number := 'ORD-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(nextval('order_number_seq')::TEXT, 6, '0');
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER set_order_number
    BEFORE INSERT ON orders
    FOR EACH ROW
    EXECUTE FUNCTION generate_order_number();