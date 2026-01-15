-- =====================================================
-- INVENTORY SERVICE DATABASE SCHEMA
-- =====================================================

-- Rezervasyon durumları için enum
CREATE TYPE reservation_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'RELEASED',
    'EXPIRED'
);

-- =====================================================
-- INVENTORIES Tablosu
-- =====================================================
CREATE TABLE inventories (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             product_id UUID NOT NULL UNIQUE,
                             product_name VARCHAR(255) NOT NULL,
                             sku VARCHAR(100) NOT NULL,
                             total_quantity INTEGER NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
                             reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
                             available_quantity INTEGER GENERATED ALWAYS AS (total_quantity - reserved_quantity) STORED,
                             min_stock_level INTEGER NOT NULL DEFAULT 10,
                             is_active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             version BIGINT NOT NULL DEFAULT 0,
                             CONSTRAINT chk_reserved_not_exceed CHECK (reserved_quantity <= total_quantity)
);

-- =====================================================
-- RESERVATIONS Tablosu
-- =====================================================
CREATE TABLE reservations (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              order_id UUID NOT NULL UNIQUE,
                              status reservation_status NOT NULL DEFAULT 'PENDING',
                              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              confirmed_at TIMESTAMP WITH TIME ZONE,
                              released_at TIMESTAMP WITH TIME ZONE,
                              release_reason VARCHAR(100),
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- RESERVATION_ITEMS Tablosu
-- =====================================================
CREATE TABLE reservation_items (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                                   product_id UUID NOT NULL,
                                   quantity INTEGER NOT NULL CHECK (quantity > 0),
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INVENTORY_MOVEMENTS Tablosu (Stok hareketleri)
-- =====================================================
CREATE TYPE movement_type AS ENUM (
    'STOCK_IN',
    'STOCK_OUT',
    'RESERVATION',
    'RESERVATION_CANCEL',
    'SALE',
    'ADJUSTMENT'
);

CREATE TABLE inventory_movements (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     product_id UUID NOT NULL,
                                     movement_type movement_type NOT NULL,
                                     quantity INTEGER NOT NULL,
                                     reference_id UUID,
                                     reference_type VARCHAR(50),
                                     notes TEXT,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
CREATE INDEX idx_inventories_product ON inventories(product_id);
CREATE INDEX idx_inventories_sku ON inventories(sku);
CREATE INDEX idx_inventories_low_stock ON inventories(available_quantity) WHERE available_quantity <= min_stock_level;
CREATE INDEX idx_reservations_order ON reservations(order_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_expires ON reservations(expires_at) WHERE status = 'PENDING';
CREATE INDEX idx_reservation_items_reservation ON reservation_items(reservation_id);
CREATE INDEX idx_inventory_movements_product ON inventory_movements(product_id);
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

CREATE TRIGGER update_inventories_updated_at
    BEFORE UPDATE ON inventories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ÖRNEK VERİ
-- =====================================================
INSERT INTO inventories (product_id, product_name, sku, total_quantity, min_stock_level) VALUES
                                                                                             ('11111111-1111-1111-1111-111111111111', 'iPhone 15 Pro', 'IPHONE-15-PRO', 100, 10),
                                                                                             ('22222222-2222-2222-2222-222222222222', 'Samsung Galaxy S24', 'SAMSUNG-S24', 150, 15),
                                                                                             ('33333333-3333-3333-3333-333333333333', 'MacBook Pro 14"', 'MACBOOK-PRO-14', 50, 5),
                                                                                             ('44444444-4444-4444-4444-444444444444', 'Dell XPS 15', 'DELL-XPS-15', 75, 10),
                                                                                             ('55555555-5555-5555-5555-555555555555', 'AirPods Pro', 'AIRPODS-PRO', 200, 20);