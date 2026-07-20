INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000110', '10000000-0000-4000-8000-000000000001', 'COMMERCIAL_REPORTS', 'Commercial Reports', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000110', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000110', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('commercial_reports:read', 'commercial_reports', 'read', 'Read commercial reports')
ON CONFLICT (code) DO NOTHING;

WITH report_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'commercial_reports:read'),
        ('TENANT_ADMIN', 'commercial_reports:read'),
        ('MANAGER', 'commercial_reports:read'),
        ('SALES_USER', 'commercial_reports:read'),
        ('STOCK_USER', 'commercial_reports:read'),
        ('READ_ONLY', 'commercial_reports:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM report_role_permissions rrp
JOIN roles r ON r.code = rrp.role_code
JOIN permissions p ON p.code = rrp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_confirmed_at
    ON sales_orders (tenant_id, confirmed_at DESC)
    WHERE deleted_at IS NULL AND status IN ('CONFIRMED', 'CANCELLED');

CREATE INDEX IF NOT EXISTS idx_purchase_orders_tenant_confirmed_at
    ON purchase_orders (tenant_id, confirmed_at DESC)
    WHERE deleted_at IS NULL AND status IN ('CONFIRMED', 'CANCELLED');

CREATE INDEX IF NOT EXISTS idx_cash_movements_tenant_occurred_at
    ON cash_movements (tenant_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_balances_tenant_quantity
    ON stock_balances (tenant_id, quantity_on_hand ASC, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_confirmed_at
    ON stock_movements (tenant_id, confirmed_at DESC);