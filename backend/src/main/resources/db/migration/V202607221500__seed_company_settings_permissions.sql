WITH company_settings_role_permissions(role_code, permission_code) AS (
    VALUES
        ('TENANT_OWNER', 'company_settings:read'),
        ('TENANT_OWNER', 'company_settings:update'),
        ('TENANT_ADMIN', 'company_settings:read'),
        ('TENANT_ADMIN', 'company_settings:update'),
        ('MANAGER', 'company_settings:read')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM company_settings_role_permissions csrp
JOIN roles r ON r.code = csrp.role_code
JOIN permissions p ON p.code = csrp.permission_code
WHERE r.tenant_id = '00000000-0000-4000-8000-000000000001'
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
