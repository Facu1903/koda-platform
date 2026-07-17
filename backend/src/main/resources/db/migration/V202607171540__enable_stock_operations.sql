ALTER TABLE stock_movements DROP CONSTRAINT ck_stock_movements_quantity;

ALTER TABLE stock_movements
    ADD COLUMN quantity_before numeric(19, 6),
    ADD COLUMN quantity_after numeric(19, 6),
    ADD COLUMN quantity_delta numeric(19, 6);

UPDATE stock_movements
SET quantity_before = CASE WHEN movement_type = 'OUT' THEN quantity ELSE 0 END,
    quantity_after = CASE WHEN movement_type = 'OUT' THEN 0 ELSE quantity END,
    quantity_delta = CASE WHEN movement_type = 'OUT' THEN -quantity ELSE quantity END;

ALTER TABLE stock_movements
    ALTER COLUMN quantity_before SET NOT NULL,
    ALTER COLUMN quantity_after SET NOT NULL,
    ALTER COLUMN quantity_delta SET NOT NULL;

ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_quantity_by_type CHECK (
        (movement_type IN ('IN', 'OUT') AND quantity > 0)
        OR (movement_type = 'ADJUSTMENT' AND quantity >= 0)
    ),
    ADD CONSTRAINT ck_stock_movements_quantity_before_non_negative CHECK (quantity_before >= 0),
    ADD CONSTRAINT ck_stock_movements_quantity_after_non_negative CHECK (quantity_after >= 0),
    ADD CONSTRAINT ck_stock_movements_delta_consistency CHECK (quantity_after = quantity_before + quantity_delta),
    ADD CONSTRAINT ck_stock_movements_delta_by_type CHECK (
        (movement_type = 'IN' AND quantity_delta = quantity)
        OR (movement_type = 'OUT' AND quantity_delta = -quantity)
        OR (movement_type = 'ADJUSTMENT' AND quantity_after = quantity)
    );

INSERT INTO permissions (code, resource, action, description) VALUES
    ('stock_balances:read', 'stock_balances', 'read', 'Read stock balances')
ON CONFLICT (code) DO NOTHING;

INSERT INTO branches (
    id, tenant_id, code, name, country_code, province_code, city, address_line, time_zone
) VALUES (
    '40000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'CENTRAL',
    'Sucursal Central',
    'AR',
    'BA',
    'Buenos Aires',
    'Casa central KODA',
    'America/Argentina/Buenos_Aires'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO warehouses (
    id, tenant_id, branch_id, code, name
) VALUES (
    '40000000-0000-4000-8000-000000000101',
    '00000000-0000-4000-8000-000000000001',
    '40000000-0000-4000-8000-000000000001',
    'PRINCIPAL',
    'Deposito Principal'
)
ON CONFLICT (id) DO NOTHING;

WITH stock_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'stock_balances:read'),
        ('TENANT_OWNER', 'stock_movements:read'),
        ('TENANT_OWNER', 'stock_movements:create'),
        ('TENANT_ADMIN', 'stock_balances:read'),
        ('TENANT_ADMIN', 'stock_movements:read'),
        ('TENANT_ADMIN', 'stock_movements:create'),
        ('MANAGER', 'stock_balances:read'),
        ('MANAGER', 'stock_movements:read'),
        ('MANAGER', 'stock_movements:create'),
        ('STOCK_USER', 'stock_balances:read'),
        ('STOCK_USER', 'stock_movements:read'),
        ('STOCK_USER', 'stock_movements:create'),
        ('SALES_USER', 'stock_balances:read'),
        ('READ_ONLY', 'stock_balances:read'),
        ('READ_ONLY', 'stock_movements:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM stock_role_permissions srp
JOIN roles r ON r.code = srp.role_code
JOIN permissions p ON p.code = srp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;