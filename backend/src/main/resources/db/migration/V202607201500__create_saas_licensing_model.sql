ALTER TABLE platform_modules
    ADD COLUMN core_module boolean DEFAULT false NOT NULL,
    ADD COLUMN commercially_toggleable boolean DEFAULT true NOT NULL;

UPDATE platform_modules
SET core_module = true,
    commercially_toggleable = false
WHERE code IN ('SECURITY', 'CONFIGURATION', 'AUDIT');

CREATE TABLE product_plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    product_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(500),
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    archived_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_product_plans PRIMARY KEY (id),
    CONSTRAINT fk_product_plans_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT uq_product_plans_product_code UNIQUE (product_id, code),
    CONSTRAINT uq_product_plans_id_product UNIQUE (id, product_id),
    CONSTRAINT ck_product_plans_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_product_plans_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_product_plans_archived_at_range CHECK (archived_at IS NULL OR archived_at >= created_at)
);

CREATE INDEX idx_product_plans_product_status ON product_plans (product_id, status);

CREATE TABLE product_plan_modules (
    plan_id uuid NOT NULL,
    product_id uuid NOT NULL,
    module_id uuid NOT NULL,
    included_by_default boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_product_plan_modules PRIMARY KEY (plan_id, module_id),
    CONSTRAINT fk_product_plan_modules_plan FOREIGN KEY (plan_id, product_id) REFERENCES product_plans (id, product_id),
    CONSTRAINT fk_product_plan_modules_module FOREIGN KEY (module_id, product_id) REFERENCES platform_modules (id, product_id)
);

CREATE INDEX idx_product_plan_modules_product ON product_plan_modules (product_id, module_id);

CREATE TABLE product_plan_limits (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    plan_id uuid NOT NULL,
    product_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    limit_value bigint,
    unlimited boolean DEFAULT false NOT NULL,
    unit varchar(40) DEFAULT 'COUNT' NOT NULL,
    description varchar(240),
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_product_plan_limits PRIMARY KEY (id),
    CONSTRAINT fk_product_plan_limits_plan FOREIGN KEY (plan_id, product_id) REFERENCES product_plans (id, product_id),
    CONSTRAINT uq_product_plan_limits_plan_code UNIQUE (plan_id, code),
    CONSTRAINT ck_product_plan_limits_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_product_plan_limits_unit CHECK (unit IN ('COUNT', 'DAYS')),
    CONSTRAINT ck_product_plan_limits_value CHECK (
        (unlimited = true AND limit_value IS NULL)
        OR (unlimited = false AND limit_value IS NOT NULL AND limit_value >= 0)
    )
);

CREATE INDEX idx_product_plan_limits_plan ON product_plan_limits (plan_id);

CREATE TABLE tenant_product_subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    valid_from timestamptz DEFAULT now() NOT NULL,
    valid_until timestamptz,
    source varchar(32) DEFAULT 'MANUAL' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    cancelled_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_product_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_tenant_product_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_product_subscriptions_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT fk_tenant_product_subscriptions_plan FOREIGN KEY (plan_id, product_id) REFERENCES product_plans (id, product_id),
    CONSTRAINT ck_tenant_product_subscriptions_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_tenant_product_subscriptions_source CHECK (source IN ('PILOT', 'MANUAL', 'SYSTEM', 'SUPPORT')),
    CONSTRAINT ck_tenant_product_subscriptions_validity CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_tenant_product_subscriptions_cancelled_at CHECK (cancelled_at IS NULL OR cancelled_at >= created_at)
);

CREATE UNIQUE INDEX uq_tenant_product_subscriptions_current
    ON tenant_product_subscriptions (tenant_id, product_id)
    WHERE status IN ('ACTIVE', 'SUSPENDED');

CREATE INDEX idx_tenant_product_subscriptions_tenant_status
    ON tenant_product_subscriptions (tenant_id, status, valid_until);

CREATE INDEX idx_tenant_product_subscriptions_plan
    ON tenant_product_subscriptions (plan_id, status);

CREATE TABLE tenant_limit_overrides (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    limit_value bigint,
    unlimited boolean DEFAULT false NOT NULL,
    unit varchar(40) DEFAULT 'COUNT' NOT NULL,
    reason varchar(240),
    valid_from timestamptz DEFAULT now() NOT NULL,
    valid_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_limit_overrides PRIMARY KEY (id),
    CONSTRAINT fk_tenant_limit_overrides_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_limit_overrides_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT uq_tenant_limit_overrides_tenant_product_code UNIQUE (tenant_id, product_id, code),
    CONSTRAINT ck_tenant_limit_overrides_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_tenant_limit_overrides_unit CHECK (unit IN ('COUNT', 'DAYS')),
    CONSTRAINT ck_tenant_limit_overrides_value CHECK (
        (unlimited = true AND limit_value IS NULL)
        OR (unlimited = false AND limit_value IS NOT NULL AND limit_value >= 0)
    ),
    CONSTRAINT ck_tenant_limit_overrides_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE INDEX idx_tenant_limit_overrides_tenant_product
    ON tenant_limit_overrides (tenant_id, product_id);

CREATE TABLE tenant_feature_flags (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    module_id uuid,
    code varchar(120) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    valid_from timestamptz DEFAULT now() NOT NULL,
    valid_until timestamptz,
    reason varchar(240),
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_feature_flags PRIMARY KEY (id),
    CONSTRAINT fk_tenant_feature_flags_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_feature_flags_product FOREIGN KEY (product_id) REFERENCES platform_products (id),
    CONSTRAINT fk_tenant_feature_flags_module FOREIGN KEY (module_id, product_id) REFERENCES platform_modules (id, product_id),
    CONSTRAINT ck_tenant_feature_flags_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_tenant_feature_flags_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE UNIQUE INDEX uq_tenant_feature_flags_scope_code
    ON tenant_feature_flags (
        tenant_id,
        product_id,
        COALESCE(module_id, '00000000-0000-0000-0000-000000000000'::uuid),
        lower(code)
    );

CREATE INDEX idx_tenant_feature_flags_tenant_product_enabled
    ON tenant_feature_flags (tenant_id, product_id, module_id)
    WHERE enabled = true;

CREATE INDEX idx_tenant_product_entitlements_tenant_status
    ON tenant_product_entitlements (tenant_id, status, valid_until);

CREATE INDEX idx_tenant_module_entitlements_tenant_status
    ON tenant_module_entitlements (tenant_id, status, valid_until);

INSERT INTO product_plans (
    id,
    product_id,
    code,
    name,
    description,
    status,
    is_system
) VALUES (
    '21000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    'KODA_PILOT',
    'KODA Pilot',
    'Internal pilot plan for KODA ERP with all current modules enabled.',
    'ACTIVE',
    true
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_plan_modules (plan_id, product_id, module_id)
SELECT
    '21000000-0000-4000-8000-000000000001',
    product_id,
    id
FROM platform_modules
WHERE product_id = '10000000-0000-4000-8000-000000000001'
  AND code IN (
      'SECURITY',
      'CONFIGURATION',
      'CATALOGS',
      'STOCK',
      'AUDIT',
      'COMMERCIAL_PARTNERS',
      'CASH',
      'SALES',
      'PURCHASES',
      'COMMERCIAL_REPORTS'
  )
ON CONFLICT DO NOTHING;

INSERT INTO product_plan_limits (
    id,
    plan_id,
    product_id,
    code,
    limit_value,
    unlimited,
    unit,
    description
) VALUES
    ('21000000-0000-4000-8000-000000000101', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'MAX_USERS', NULL, true, 'COUNT', 'Maximum active users allowed by the plan.'),
    ('21000000-0000-4000-8000-000000000102', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'MAX_BRANCHES', NULL, true, 'COUNT', 'Maximum active branches allowed by the plan.'),
    ('21000000-0000-4000-8000-000000000103', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'MAX_WAREHOUSES', NULL, true, 'COUNT', 'Maximum active warehouses allowed by the plan.'),
    ('21000000-0000-4000-8000-000000000104', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'MAX_PRODUCTS', NULL, true, 'COUNT', 'Maximum active products allowed by the plan.'),
    ('21000000-0000-4000-8000-000000000105', '21000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'AUDIT_RETENTION_DAYS', NULL, true, 'DAYS', 'Audit retention in days allowed by the plan.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_product_subscriptions (
    id,
    tenant_id,
    product_id,
    plan_id,
    status,
    source
) VALUES (
    '22000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    '21000000-0000-4000-8000-000000000001',
    'ACTIVE',
    'PILOT'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO tenant_product_entitlements (tenant_id, product_id, status)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001',
    'ACTIVE'
)
ON CONFLICT (tenant_id, product_id) DO NOTHING;

INSERT INTO tenant_module_entitlements (tenant_id, module_id, status)
SELECT
    '00000000-0000-4000-8000-000000000001',
    module_id,
    'ACTIVE'
FROM product_plan_modules
WHERE plan_id = '21000000-0000-4000-8000-000000000001'
ON CONFLICT (tenant_id, module_id) DO NOTHING;