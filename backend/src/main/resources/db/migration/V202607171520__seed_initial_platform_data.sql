INSERT INTO tenants (
    id,
    commercial_name,
    legal_name,
    status,
    country_code,
    default_locale,
    default_currency,
    time_zone
) VALUES (
    '00000000-0000-4000-8000-000000000001',
    'KODA',
    'KODA',
    'ACTIVE',
    'AR',
    'es-AR',
    'ARS',
    'America/Argentina/Buenos_Aires'
);

INSERT INTO company_settings (
    id,
    tenant_id,
    primary_color,
    theme_mode,
    date_format,
    time_format,
    number_locale,
    currency_format
) VALUES (
    '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000001',
    '#F6862B',
    'dark',
    'dd/MM/yyyy',
    'HH:mm',
    'es-AR',
    'symbol'
);

INSERT INTO platform_products (id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000001', 'KODA_ERP', 'KODA ERP', 'ACTIVE');

INSERT INTO platform_modules (id, product_id, code, name, status) VALUES
    ('10000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000001', 'SECURITY', 'Security', 'ACTIVE'),
    ('10000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000001', 'CONFIGURATION', 'Configuration', 'ACTIVE'),
    ('10000000-0000-4000-8000-000000000103', '10000000-0000-4000-8000-000000000001', 'CATALOGS', 'Catalogs', 'ACTIVE'),
    ('10000000-0000-4000-8000-000000000104', '10000000-0000-4000-8000-000000000001', 'STOCK', 'Stock', 'ACTIVE'),
    ('10000000-0000-4000-8000-000000000105', '10000000-0000-4000-8000-000000000001', 'AUDIT', 'Audit', 'ACTIVE');

INSERT INTO tenant_product_entitlements (id, tenant_id, product_id, status) VALUES
    ('20000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'ACTIVE');

INSERT INTO tenant_module_entitlements (id, tenant_id, module_id, status) VALUES
    ('20000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000102', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000103', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000104', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000104', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000105', '00000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000105', 'ACTIVE');

INSERT INTO roles (id, tenant_id, code, name, scope, is_system, is_assignable) VALUES
    ('30000000-0000-4000-8000-000000000001', NULL, 'PLATFORM_SUPER_ADMIN', 'Platform Super Admin', 'PLATFORM', true, true),
    ('30000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001', 'TENANT_OWNER', 'Tenant Owner', 'TENANT', true, true),
    ('30000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001', 'TENANT_ADMIN', 'Tenant Admin', 'TENANT', true, true),
    ('30000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001', 'MANAGER', 'Manager', 'TENANT', true, true),
    ('30000000-0000-4000-8000-000000000005', '00000000-0000-4000-8000-000000000001', 'SALES_USER', 'Sales User', 'TENANT', true, true),
    ('30000000-0000-4000-8000-000000000006', '00000000-0000-4000-8000-000000000001', 'STOCK_USER', 'Stock User', 'TENANT', true, true),
    ('30000000-0000-4000-8000-000000000007', '00000000-0000-4000-8000-000000000001', 'READ_ONLY', 'Read Only', 'TENANT', true, true);

INSERT INTO permissions (code, resource, action, description) VALUES
    ('users:read', 'users', 'read', 'Read users'),
    ('users:create', 'users', 'create', 'Create users'),
    ('users:update', 'users', 'update', 'Update users'),
    ('users:delete', 'users', 'delete', 'Delete users'),
    ('roles:read', 'roles', 'read', 'Read roles'),
    ('roles:create', 'roles', 'create', 'Create roles'),
    ('roles:update', 'roles', 'update', 'Update roles'),
    ('roles:delete', 'roles', 'delete', 'Delete roles'),
    ('tenants:read', 'tenants', 'read', 'Read tenants'),
    ('tenants:create', 'tenants', 'create', 'Create tenants'),
    ('tenants:update', 'tenants', 'update', 'Update tenants'),
    ('branches:read', 'branches', 'read', 'Read branches'),
    ('branches:create', 'branches', 'create', 'Create branches'),
    ('branches:update', 'branches', 'update', 'Update branches'),
    ('branches:delete', 'branches', 'delete', 'Delete branches'),
    ('company_settings:read', 'company_settings', 'read', 'Read company settings'),
    ('company_settings:update', 'company_settings', 'update', 'Update company settings'),
    ('products:read', 'products', 'read', 'Read products'),
    ('products:create', 'products', 'create', 'Create products'),
    ('products:update', 'products', 'update', 'Update products'),
    ('products:delete', 'products', 'delete', 'Delete products'),
    ('brands:read', 'brands', 'read', 'Read brands'),
    ('brands:create', 'brands', 'create', 'Create brands'),
    ('brands:update', 'brands', 'update', 'Update brands'),
    ('brands:delete', 'brands', 'delete', 'Delete brands'),
    ('categories:read', 'categories', 'read', 'Read categories'),
    ('categories:create', 'categories', 'create', 'Create categories'),
    ('categories:update', 'categories', 'update', 'Update categories'),
    ('categories:delete', 'categories', 'delete', 'Delete categories'),
    ('units:read', 'units', 'read', 'Read units'),
    ('units:create', 'units', 'create', 'Create units'),
    ('units:update', 'units', 'update', 'Update units'),
    ('units:delete', 'units', 'delete', 'Delete units'),
    ('presentations:read', 'presentations', 'read', 'Read presentations'),
    ('presentations:create', 'presentations', 'create', 'Create presentations'),
    ('presentations:update', 'presentations', 'update', 'Update presentations'),
    ('presentations:delete', 'presentations', 'delete', 'Delete presentations'),
    ('stock_movements:read', 'stock_movements', 'read', 'Read stock movements'),
    ('stock_movements:create', 'stock_movements', 'create', 'Create stock movements'),
    ('audit:read', 'audit', 'read', 'Read audit events');