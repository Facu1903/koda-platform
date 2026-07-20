CREATE TABLE business_partners (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    legal_name varchar(220) NOT NULL,
    commercial_name varchar(220),
    document_type varchar(40),
    document_number varchar(80),
    tax_condition varchar(80),
    email varchar(254),
    phone varchar(80),
    address_line varchar(240),
    city varchar(120),
    province_code varchar(80),
    country_code char(2),
    notes varchar(1000),
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_business_partners PRIMARY KEY (id),
    CONSTRAINT fk_business_partners_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_business_partners_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_business_partners_legal_name_not_blank CHECK (length(trim(legal_name)) > 0),
    CONSTRAINT ck_business_partners_document_pair CHECK ((document_type IS NULL AND document_number IS NULL) OR (document_type IS NOT NULL AND document_number IS NOT NULL)),
    CONSTRAINT ck_business_partners_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_business_partners_country_code CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_business_partners_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_business_partners_tenant_document_active
    ON business_partners (tenant_id, lower(document_type), lower(document_number))
    WHERE document_type IS NOT NULL AND document_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_business_partners_tenant_status ON business_partners (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_business_partners_tenant_name ON business_partners (tenant_id, lower(legal_name)) WHERE deleted_at IS NULL;

CREATE TABLE business_partner_roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    business_partner_id uuid NOT NULL,
    role_type varchar(32) NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    CONSTRAINT pk_business_partner_roles PRIMARY KEY (id),
    CONSTRAINT fk_business_partner_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_business_partner_roles_partner FOREIGN KEY (business_partner_id, tenant_id) REFERENCES business_partners (id, tenant_id),
    CONSTRAINT uq_business_partner_roles_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_business_partner_roles_type CHECK (role_type IN ('CUSTOMER', 'SUPPLIER')),
    CONSTRAINT ck_business_partner_roles_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_business_partner_roles_active
    ON business_partner_roles (tenant_id, business_partner_id, role_type)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_business_partner_roles_tenant_role ON business_partner_roles (tenant_id, role_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_business_partner_roles_partner ON business_partner_roles (tenant_id, business_partner_id) WHERE deleted_at IS NULL;