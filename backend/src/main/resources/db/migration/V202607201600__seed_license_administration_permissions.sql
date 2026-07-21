INSERT INTO permissions (code, resource, action, description) VALUES
    ('license_admin:read', 'license_admin', 'read', 'Read tenant license administration state'),
    ('license_admin:update', 'license_admin', 'update', 'Update tenant license subscriptions and entitlements')
ON CONFLICT (code) DO NOTHING;

WITH platform_license_role_permissions(role_code, permission_code) AS (
    VALUES
        ('PLATFORM_SUPER_ADMIN', 'license_admin:read'),
        ('PLATFORM_SUPER_ADMIN', 'license_admin:update')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM platform_license_role_permissions plrp
JOIN roles r ON r.code = plrp.role_code AND r.scope = 'PLATFORM' AND r.deleted_at IS NULL
JOIN permissions p ON p.code = plrp.permission_code
ON CONFLICT DO NOTHING;
