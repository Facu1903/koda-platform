INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000106', '10000000-0000-4000-8000-000000000001', 'COMMERCIAL_PARTNERS', 'Commercial Partners', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000106', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000106', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO business_partners (
    id, tenant_id, legal_name, commercial_name, status, is_system
) VALUES (
    '50000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'Consumidor Final',
    'Consumidor Final',
    'ACTIVE',
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO business_partner_roles (
    id, tenant_id, business_partner_id, role_type, is_system
) VALUES (
    '50000000-0000-4000-8000-000000000101',
    '00000000-0000-4000-8000-000000000001',
    '50000000-0000-4000-8000-000000000001',
    'CUSTOMER',
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('customers:read', 'customers', 'read', 'Read customers'),
    ('customers:create', 'customers', 'create', 'Create customers'),
    ('customers:update', 'customers', 'update', 'Update customers'),
    ('customers:delete', 'customers', 'delete', 'Delete customers'),
    ('suppliers:read', 'suppliers', 'read', 'Read suppliers'),
    ('suppliers:create', 'suppliers', 'create', 'Create suppliers'),
    ('suppliers:update', 'suppliers', 'update', 'Update suppliers'),
    ('suppliers:delete', 'suppliers', 'delete', 'Delete suppliers')
ON CONFLICT (code) DO NOTHING;

WITH commercial_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'customers:read'),
        ('TENANT_OWNER', 'customers:create'),
        ('TENANT_OWNER', 'customers:update'),
        ('TENANT_OWNER', 'customers:delete'),
        ('TENANT_OWNER', 'suppliers:read'),
        ('TENANT_OWNER', 'suppliers:create'),
        ('TENANT_OWNER', 'suppliers:update'),
        ('TENANT_OWNER', 'suppliers:delete'),
        ('TENANT_ADMIN', 'customers:read'),
        ('TENANT_ADMIN', 'customers:create'),
        ('TENANT_ADMIN', 'customers:update'),
        ('TENANT_ADMIN', 'customers:delete'),
        ('TENANT_ADMIN', 'suppliers:read'),
        ('TENANT_ADMIN', 'suppliers:create'),
        ('TENANT_ADMIN', 'suppliers:update'),
        ('TENANT_ADMIN', 'suppliers:delete'),
        ('MANAGER', 'customers:read'),
        ('MANAGER', 'customers:create'),
        ('MANAGER', 'customers:update'),
        ('MANAGER', 'customers:delete'),
        ('MANAGER', 'suppliers:read'),
        ('MANAGER', 'suppliers:create'),
        ('MANAGER', 'suppliers:update'),
        ('MANAGER', 'suppliers:delete'),
        ('SALES_USER', 'customers:read'),
        ('SALES_USER', 'customers:create'),
        ('SALES_USER', 'customers:update'),
        ('SALES_USER', 'suppliers:read'),
        ('STOCK_USER', 'customers:read'),
        ('STOCK_USER', 'suppliers:read'),
        ('STOCK_USER', 'suppliers:create'),
        ('STOCK_USER', 'suppliers:update'),
        ('READ_ONLY', 'customers:read'),
        ('READ_ONLY', 'suppliers:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM commercial_role_permissions crp
JOIN roles r ON r.code = crp.role_code
JOIN permissions p ON p.code = crp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;