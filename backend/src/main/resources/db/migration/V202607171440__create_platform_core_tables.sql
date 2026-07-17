CREATE TABLE tenants (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    commercial_name varchar(160) NOT NULL,
    legal_name varchar(200) NOT NULL,
    tax_identifier varchar(64),
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    country_code char(2) NOT NULL,
    default_locale varchar(16) NOT NULL,
    default_currency char(3) NOT NULL,
    time_zone varchar(64) NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED')),
    CONSTRAINT ck_tenants_country_code_format CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_tenants_currency_code_format CHECK (default_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_tenants_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_tenants_commercial_name_active ON tenants (lower(commercial_name)) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_status ON tenants (status) WHERE deleted_at IS NULL;

CREATE TABLE company_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    logo_url varchar(2048),
    favicon_url varchar(2048),
    login_image_url varchar(2048),
    primary_color varchar(7) DEFAULT '#F6862B' NOT NULL,
    secondary_color varchar(7),
    theme_mode varchar(16) DEFAULT 'dark' NOT NULL,
    date_format varchar(32) DEFAULT 'dd/MM/yyyy' NOT NULL,
    time_format varchar(32) DEFAULT 'HH:mm' NOT NULL,
    number_locale varchar(16) DEFAULT 'es-AR' NOT NULL,
    currency_format varchar(32) DEFAULT 'symbol' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_company_settings PRIMARY KEY (id),
    CONSTRAINT fk_company_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_company_settings_tenant UNIQUE (tenant_id),
    CONSTRAINT ck_company_settings_primary_color CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_company_settings_secondary_color CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_company_settings_theme_mode CHECK (theme_mode IN ('light', 'dark', 'system'))
);

CREATE TABLE platform_products (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(160) NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_platform_products PRIMARY KEY (id),
    CONSTRAINT uq_platform_products_code UNIQUE (code),
    CONSTRAINT ck_platform_products_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED'))
);

CREATE TABLE platform_modules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(160) NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_platform_modules PRIMARY KEY (id),
    CONSTRAINT fk_platform_modules_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT uq_platform_modules_product_code UNIQUE (product_id, code),
    CONSTRAINT uq_platform_modules_id_product UNIQUE (id, product_id),
    CONSTRAINT ck_platform_modules_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED'))
);

CREATE TABLE tenant_product_entitlements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    valid_from timestamptz DEFAULT now() NOT NULL,
    valid_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_product_entitlements PRIMARY KEY (id),
    CONSTRAINT fk_tenant_product_entitlements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_product_entitlements_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT uq_tenant_product_entitlements_tenant_product UNIQUE (tenant_id, product_id),
    CONSTRAINT ck_tenant_product_entitlements_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT ck_tenant_product_entitlements_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE TABLE tenant_module_entitlements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    module_id uuid NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    valid_from timestamptz DEFAULT now() NOT NULL,
    valid_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_module_entitlements PRIMARY KEY (id),
    CONSTRAINT fk_tenant_module_entitlements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_module_entitlements_module FOREIGN KEY (module_id) REFERENCES platform_modules (id),
    CONSTRAINT uq_tenant_module_entitlements_tenant_module UNIQUE (tenant_id, module_id),
    CONSTRAINT ck_tenant_module_entitlements_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT ck_tenant_module_entitlements_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE TABLE branches (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    country_code char(2) NOT NULL,
    province_code varchar(32),
    city varchar(120),
    address_line varchar(240),
    time_zone varchar(64),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id),
    CONSTRAINT fk_branches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_branches_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_branches_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_branches_country_code_format CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_branches_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_branches_tenant_code_active ON branches (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_branches_tenant_active ON branches (tenant_id, is_active) WHERE deleted_at IS NULL;

CREATE TABLE warehouses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_warehouses PRIMARY KEY (id),
    CONSTRAINT fk_warehouses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_warehouses_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT uq_warehouses_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_warehouses_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_warehouses_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_warehouses_tenant_code_active ON warehouses (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_warehouses_tenant_branch ON warehouses (tenant_id, branch_id) WHERE deleted_at IS NULL;