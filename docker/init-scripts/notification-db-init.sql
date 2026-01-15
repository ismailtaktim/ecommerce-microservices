-- =====================================================
-- NOTIFICATION SERVICE DATABASE SCHEMA
-- =====================================================

-- Bildirim türleri için enum
CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED',
    'ORDER_COMPLETED',
    'ORDER_CANCELLED',
    'PAYMENT_SUCCESS',
    'PAYMENT_FAILED',
    'INVENTORY_LOW'
);

-- Bildirim kanalları için enum
CREATE TYPE notification_channel AS ENUM (
    'EMAIL',
    'SMS'
);

-- Bildirim durumları için enum
CREATE TYPE notification_status AS ENUM (
    'PENDING',
    'SENDING',
    'SENT',
    'FAILED',
    'RETRY_PENDING'
);

-- =====================================================
-- NOTIFICATIONS Tablosu
-- =====================================================
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               order_id UUID,
                               customer_id UUID NOT NULL,
                               type notification_type NOT NULL,
                               channel notification_channel NOT NULL,
                               recipient VARCHAR(255) NOT NULL,
                               subject VARCHAR(500),
                               content TEXT NOT NULL,
                               status notification_status NOT NULL DEFAULT 'PENDING',
                               retry_count INTEGER NOT NULL DEFAULT 0,
                               max_retries INTEGER NOT NULL DEFAULT 3,
                               next_retry_at TIMESTAMP WITH TIME ZONE,
                               sent_at TIMESTAMP WITH TIME ZONE,
                               failed_reason TEXT,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- NOTIFICATION_TEMPLATES Tablosu
-- =====================================================
CREATE TABLE notification_templates (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        type notification_type NOT NULL,
                                        channel notification_channel NOT NULL,
                                        subject VARCHAR(500),
                                        content TEXT NOT NULL,
                                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                        UNIQUE(type, channel)
);

-- =====================================================
-- NOTIFICATION_LOGS Tablosu
-- =====================================================
CREATE TABLE notification_logs (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
                                   status notification_status NOT NULL,
                                   response_code VARCHAR(20),
                                   response_message TEXT,
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
CREATE INDEX idx_notifications_order ON notifications(order_id);
CREATE INDEX idx_notifications_customer ON notifications(customer_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_retry ON notifications(next_retry_at) WHERE status = 'RETRY_PENDING';
CREATE INDEX idx_notification_logs_notification ON notification_logs(notification_id);
CREATE INDEX idx_templates_type_channel ON notification_templates(type, channel);
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

CREATE TRIGGER update_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ÖRNEK ŞABLONLAR
-- =====================================================
INSERT INTO notification_templates (type, channel, subject, content) VALUES
                                                                         ('ORDER_CREATED', 'EMAIL', 'Siparişiniz Alındı - #{orderNumber}', 'Sayın #{customerName}, #{orderNumber} numaralı siparişiniz başarıyla oluşturuldu. Toplam tutar: #{totalAmount} TL'),
                                                                         ('ORDER_CREATED', 'SMS', NULL, 'Siparişiniz alındı. Sipariş No: #{orderNumber}'),
                                                                         ('ORDER_COMPLETED', 'EMAIL', 'Siparişiniz Tamamlandı - #{orderNumber}', 'Sayın #{customerName}, #{orderNumber} numaralı siparişiniz tamamlandı.'),
                                                                         ('ORDER_COMPLETED', 'SMS', NULL, 'Siparişiniz tamamlandı. Sipariş No: #{orderNumber}'),
                                                                         ('ORDER_CANCELLED', 'EMAIL', 'Siparişiniz İptal Edildi - #{orderNumber}', 'Sayın #{customerName}, #{orderNumber} numaralı siparişiniz iptal edilmiştir. İptal nedeni: #{reason}'),
                                                                         ('PAYMENT_SUCCESS', 'EMAIL', 'Ödemeniz Alındı - #{orderNumber}', 'Sayın #{customerName}, #{amount} TL tutarındaki ödemeniz başarıyla alınmıştır.'),
                                                                         ('PAYMENT_FAILED', 'EMAIL', 'Ödeme Başarısız - #{orderNumber}', 'Sayın #{customerName}, ödemeniz gerçekleştirilemedi. Lütfen tekrar deneyiniz.');