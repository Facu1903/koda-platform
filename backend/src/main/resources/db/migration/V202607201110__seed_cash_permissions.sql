INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000107', '10000000-0000-4000-8000-000000000001', 'CASH', 'Cash', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000107', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000107', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cash_registers (
    id, tenant_id, branch_id, code, name, status
) VALUES (
    '60000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '40000000-0000-4000-8000-000000000001',
    'CAJA_PRINCIPAL',
    'Caja Principal',
    'ACTIVE'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('cash_registers:read', 'cash_registers', 'read', 'Read cash registers'),
    ('cash_sessions:read', 'cash_sessions', 'read', 'Read cash sessions'),
    ('cash_sessions:open', 'cash_sessions', 'open', 'Open cash sessions'),
    ('cash_sessions:close', 'cash_sessions', 'close', 'Close cash sessions'),
    ('cash_movements:read', 'cash_movements', 'read', 'Read cash movements'),
    ('cash_movements:create', 'cash_movements', 'create', 'Create cash movements')
ON CONFLICT (code) DO NOTHING;

WITH cash_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'cash_registers:read'),
        ('TENANT_OWNER', 'cash_sessions:read'),
        ('TENANT_OWNER', 'cash_sessions:open'),
        ('TENANT_OWNER', 'cash_sessions:close'),
        ('TENANT_OWNER', 'cash_movements:read'),
        ('TENANT_OWNER', 'cash_movements:create'),
        ('TENANT_ADMIN', 'cash_registers:read'),
        ('TENANT_ADMIN', 'cash_sessions:read'),
        ('TENANT_ADMIN', 'cash_sessions:open'),
        ('TENANT_ADMIN', 'cash_sessions:close'),
        ('TENANT_ADMIN', 'cash_movements:read'),
        ('TENANT_ADMIN', 'cash_movements:create'),
        ('MANAGER', 'cash_registers:read'),
        ('MANAGER', 'cash_sessions:read'),
        ('MANAGER', 'cash_sessions:open'),
        ('MANAGER', 'cash_sessions:close'),
        ('MANAGER', 'cash_movements:read'),
        ('MANAGER', 'cash_movements:create'),
        ('SALES_USER', 'cash_registers:read'),
        ('SALES_USER', 'cash_sessions:read'),
        ('SALES_USER', 'cash_sessions:open'),
        ('SALES_USER', 'cash_sessions:close'),
        ('SALES_USER', 'cash_movements:read'),
        ('SALES_USER', 'cash_movements:create'),
        ('READ_ONLY', 'cash_registers:read'),
        ('READ_ONLY', 'cash_sessions:read'),
        ('READ_ONLY', 'cash_movements:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM cash_role_permissions crp
JOIN roles r ON r.code = crp.role_code
JOIN permissions p ON p.code = crp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;