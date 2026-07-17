CREATE TABLE brands (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(500),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT fk_brands_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_brands_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_brands_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_brands_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_brands_tenant_code_active ON brands (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_brands_tenant_active ON brands (tenant_id, is_active) WHERE deleted_at IS NULL;

CREATE TABLE categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    parent_id uuid,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(500),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id, tenant_id) REFERENCES categories (id, tenant_id),
    CONSTRAINT uq_categories_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_categories_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_categories_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_categories_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_categories_tenant_code_active ON categories (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_tenant_parent ON categories (tenant_id, parent_id) WHERE deleted_at IS NULL;

CREATE TABLE units_of_measure (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(120) NOT NULL,
    symbol varchar(24) NOT NULL,
    decimal_precision smallint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_units_of_measure PRIMARY KEY (id),
    CONSTRAINT fk_units_of_measure_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_units_of_measure_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_units_of_measure_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_units_of_measure_decimal_precision CHECK (decimal_precision BETWEEN 0 AND 6),
    CONSTRAINT ck_units_of_measure_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_units_of_measure_tenant_code_active ON units_of_measure (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_units_of_measure_tenant_active ON units_of_measure (tenant_id, is_active) WHERE deleted_at IS NULL;

CREATE TABLE product_presentations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_product_presentations PRIMARY KEY (id),
    CONSTRAINT fk_product_presentations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_product_presentations_unit FOREIGN KEY (unit_id, tenant_id) REFERENCES units_of_measure (id, tenant_id),
    CONSTRAINT uq_product_presentations_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_product_presentations_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_product_presentations_quantity CHECK (quantity > 0),
    CONSTRAINT ck_product_presentations_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_product_presentations_tenant_code_active ON product_presentations (tenant_id, lower(code)) WHERE deleted_at IS NULL;
CREATE INDEX idx_product_presentations_tenant_unit ON product_presentations (tenant_id, unit_id) WHERE deleted_at IS NULL;

CREATE TABLE products (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    sku varchar(80) NOT NULL,
    name varchar(220) NOT NULL,
    description varchar(1000),
    barcode varchar(80),
    brand_id uuid,
    category_id uuid,
    base_unit_id uuid NOT NULL,
    default_presentation_id uuid,
    product_type varchar(32) DEFAULT 'GOOD' NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    stock_tracking_enabled boolean DEFAULT true NOT NULL,
    allow_negative_stock boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id, tenant_id) REFERENCES brands (id, tenant_id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id, tenant_id) REFERENCES categories (id, tenant_id),
    CONSTRAINT fk_products_base_unit FOREIGN KEY (base_unit_id, tenant_id) REFERENCES units_of_measure (id, tenant_id),
    CONSTRAINT fk_products_default_presentation FOREIGN KEY (default_presentation_id, tenant_id) REFERENCES product_presentations (id, tenant_id),
    CONSTRAINT uq_products_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_products_sku_not_blank CHECK (length(trim(sku)) > 0),
    CONSTRAINT ck_products_product_type CHECK (product_type IN ('GOOD', 'SERVICE')),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_products_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_products_tenant_sku_active ON products (tenant_id, lower(sku)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_products_tenant_barcode_active ON products (tenant_id, barcode) WHERE barcode IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_products_tenant_status ON products (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_tenant_brand ON products (tenant_id, brand_id) WHERE brand_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_products_tenant_category ON products (tenant_id, category_id) WHERE category_id IS NOT NULL AND deleted_at IS NULL;