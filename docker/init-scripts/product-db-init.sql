-- =====================================================
-- PRODUCT SERVICE DATABASE SCHEMA
-- =====================================================

-- Ürün durumları için enum
CREATE TYPE product_status AS ENUM (
    'DRAFT',
    'ACTIVE',
    'INACTIVE',
    'DISCONTINUED'
);

-- =====================================================
-- CATEGORIES Tablosu
-- =====================================================
CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) NOT NULL,
                            slug VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
                            image_url VARCHAR(500),
                            sort_order INTEGER NOT NULL DEFAULT 0,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            level INTEGER NOT NULL DEFAULT 0,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PRODUCTS Tablosu
-- =====================================================
CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          sku VARCHAR(100) NOT NULL UNIQUE,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) NOT NULL UNIQUE,
                          short_description VARCHAR(500),
                          description TEXT,
                          status product_status NOT NULL DEFAULT 'DRAFT',
                          price DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
                          list_price DECIMAL(15, 2) CHECK (list_price >= 0),
                          currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                          tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 18.00,
                          category_id UUID NOT NULL REFERENCES categories(id),
                          weight_grams INTEGER,
                          width_cm DECIMAL(10, 2),
                          height_cm DECIMAL(10, 2),
                          depth_cm DECIMAL(10, 2),
                          main_image_url VARCHAR(500),
                          created_by UUID,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          version BIGINT NOT NULL DEFAULT 0,
                          CONSTRAINT chk_price_valid CHECK (list_price IS NULL OR list_price >= price)
);

-- =====================================================
-- PRODUCT_IMAGES Tablosu
-- =====================================================
CREATE TABLE product_images (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                image_url VARCHAR(500) NOT NULL,
                                alt_text VARCHAR(255),
                                sort_order INTEGER NOT NULL DEFAULT 0,
                                is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INDEXES
-- =====================================================
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_product_images_product ON product_images(product_id);

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

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ÖRNEK VERİ
-- =====================================================

-- Kategoriler
INSERT INTO categories (id, name, slug, description, sort_order, is_active) VALUES
                                                                                ('10000000-0000-0000-0000-000000000001', 'Elektronik', 'elektronik', 'Elektronik ürünler', 1, TRUE),
                                                                                ('10000000-0000-0000-0000-000000000002', 'Bilgisayar', 'bilgisayar', 'Bilgisayar ve aksesuarları', 2, TRUE),
                                                                                ('10000000-0000-0000-0000-000000000003', 'Telefon', 'telefon', 'Cep telefonları', 3, TRUE);

-- Alt Kategoriler
INSERT INTO categories (id, name, slug, description, parent_id, sort_order, level, is_active) VALUES
                                                                                                  ('11000000-0000-0000-0000-000000000001', 'Laptop', 'laptop', 'Dizüstü bilgisayarlar', '10000000-0000-0000-0000-000000000002', 1, 1, TRUE),
                                                                                                  ('11000000-0000-0000-0000-000000000002', 'Akıllı Telefonlar', 'akilli-telefonlar', 'Smartphone', '10000000-0000-0000-0000-000000000003', 1, 1, TRUE);

-- Ürünler
INSERT INTO products (id, sku, name, slug, short_description, status, price, list_price, category_id) VALUES
                                                                                                          ('11111111-1111-1111-1111-111111111111', 'IPHONE-15-PRO', 'iPhone 15 Pro', 'iphone-15-pro', 'Apple iPhone 15 Pro 256GB', 'ACTIVE', 64999.00, 69999.00, '11000000-0000-0000-0000-000000000002'),
                                                                                                          ('22222222-2222-2222-2222-222222222222', 'SAMSUNG-S24', 'Samsung Galaxy S24', 'samsung-galaxy-s24', 'Samsung Galaxy S24 256GB', 'ACTIVE', 47999.00, NULL, '11000000-0000-0000-0000-000000000002'),
                                                                                                          ('33333333-3333-3333-3333-333333333333', 'MACBOOK-PRO-14', 'MacBook Pro 14"', 'macbook-pro-14', 'Apple MacBook Pro 14 M3 Pro', 'ACTIVE', 84999.00, 89999.00, '11000000-0000-0000-0000-000000000001'),
                                                                                                          ('44444444-4444-4444-4444-444444444444', 'DELL-XPS-15', 'Dell XPS 15', 'dell-xps-15', 'Dell XPS 15 Intel Core i7', 'ACTIVE', 54999.00, NULL, '11000000-0000-0000-0000-000000000001'),
                                                                                                          ('55555555-5555-5555-5555-555555555555', 'AIRPODS-PRO', 'AirPods Pro', 'airpods-pro', 'Apple AirPods Pro 2. Nesil', 'ACTIVE', 8999.00, NULL, '10000000-0000-0000-0000-000000000001');