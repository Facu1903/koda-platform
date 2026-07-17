WITH audit_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'audit:read'),
        ('TENANT_ADMIN', 'audit:read'),
        ('MANAGER', 'audit:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM audit_role_permissions arp
JOIN roles r ON r.code = arp.role_code
JOIN permissions p ON p.code = arp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;