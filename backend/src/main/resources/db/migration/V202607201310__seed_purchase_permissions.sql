INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000109', '10000000-0000-4000-8000-000000000001', 'PURCHASES', 'Purchases', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000109', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000109', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('purchases:read', 'purchases', 'read', 'Read purchases'),
    ('purchases:create', 'purchases', 'create', 'Create purchases'),
    ('purchases:update', 'purchases', 'update', 'Update draft purchases'),
    ('purchases:delete', 'purchases', 'delete', 'Delete draft purchases'),
    ('purchases:confirm', 'purchases', 'confirm', 'Confirm purchases'),
    ('purchases:cancel', 'purchases', 'cancel', 'Cancel confirmed purchases')
ON CONFLICT (code) DO NOTHING;

WITH purchase_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'purchases:read'),
        ('TENANT_OWNER', 'purchases:create'),
        ('TENANT_OWNER', 'purchases:update'),
        ('TENANT_OWNER', 'purchases:delete'),
        ('TENANT_OWNER', 'purchases:confirm'),
        ('TENANT_OWNER', 'purchases:cancel'),
        ('TENANT_ADMIN', 'purchases:read'),
        ('TENANT_ADMIN', 'purchases:create'),
        ('TENANT_ADMIN', 'purchases:update'),
        ('TENANT_ADMIN', 'purchases:delete'),
        ('TENANT_ADMIN', 'purchases:confirm'),
        ('TENANT_ADMIN', 'purchases:cancel'),
        ('MANAGER', 'purchases:read'),
        ('MANAGER', 'purchases:create'),
        ('MANAGER', 'purchases:update'),
        ('MANAGER', 'purchases:delete'),
        ('MANAGER', 'purchases:confirm'),
        ('MANAGER', 'purchases:cancel'),
        ('STOCK_USER', 'purchases:read'),
        ('STOCK_USER', 'purchases:create'),
        ('STOCK_USER', 'purchases:update'),
        ('SALES_USER', 'purchases:read'),
        ('READ_ONLY', 'purchases:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM purchase_role_permissions prp
JOIN roles r ON r.code = prp.role_code
JOIN permissions p ON p.code = prp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;