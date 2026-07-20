INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000108', '10000000-0000-4000-8000-000000000001', 'SALES', 'Sales', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000108', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000108', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('sales:read', 'sales', 'read', 'Read sales'),
    ('sales:create', 'sales', 'create', 'Create sales'),
    ('sales:update', 'sales', 'update', 'Update draft sales'),
    ('sales:delete', 'sales', 'delete', 'Delete draft sales'),
    ('sales:confirm', 'sales', 'confirm', 'Confirm sales'),
    ('sales:cancel', 'sales', 'cancel', 'Cancel confirmed sales')
ON CONFLICT (code) DO NOTHING;

WITH sales_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'sales:read'),
        ('TENANT_OWNER', 'sales:create'),
        ('TENANT_OWNER', 'sales:update'),
        ('TENANT_OWNER', 'sales:delete'),
        ('TENANT_OWNER', 'sales:confirm'),
        ('TENANT_OWNER', 'sales:cancel'),
        ('TENANT_ADMIN', 'sales:read'),
        ('TENANT_ADMIN', 'sales:create'),
        ('TENANT_ADMIN', 'sales:update'),
        ('TENANT_ADMIN', 'sales:delete'),
        ('TENANT_ADMIN', 'sales:confirm'),
        ('TENANT_ADMIN', 'sales:cancel'),
        ('MANAGER', 'sales:read'),
        ('MANAGER', 'sales:create'),
        ('MANAGER', 'sales:update'),
        ('MANAGER', 'sales:delete'),
        ('MANAGER', 'sales:confirm'),
        ('MANAGER', 'sales:cancel'),
        ('SALES_USER', 'sales:read'),
        ('SALES_USER', 'sales:create'),
        ('SALES_USER', 'sales:update'),
        ('SALES_USER', 'sales:confirm'),
        ('STOCK_USER', 'sales:read'),
        ('READ_ONLY', 'sales:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sales_role_permissions srp
JOIN roles r ON r.code = srp.role_code
JOIN permissions p ON p.code = srp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;