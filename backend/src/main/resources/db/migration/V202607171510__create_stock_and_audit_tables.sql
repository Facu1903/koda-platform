CREATE TABLE stock_balances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity_on_hand numeric(19, 6) DEFAULT 0 NOT NULL,
    reserved_quantity numeric(19, 6) DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_stock_balances PRIMARY KEY (id),
    CONSTRAINT fk_stock_balances_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_balances_warehouse FOREIGN KEY (warehouse_id, tenant_id) REFERENCES warehouses (id, tenant_id),
    CONSTRAINT fk_stock_balances_product FOREIGN KEY (product_id, tenant_id) REFERENCES products (id, tenant_id),
    CONSTRAINT uq_stock_balances_tenant_warehouse_product UNIQUE (tenant_id, warehouse_id, product_id),
    CONSTRAINT ck_stock_balances_quantity_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_stock_balances_reserved_quantity CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_stock_balances_reserved_not_greater CHECK (reserved_quantity <= quantity_on_hand)
);

CREATE INDEX idx_stock_balances_tenant_product ON stock_balances (tenant_id, product_id);

CREATE TABLE stock_movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    product_id uuid NOT NULL,
    movement_type varchar(32) NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    unit_cost numeric(19, 4),
    currency_code char(3),
    reference_type varchar(80),
    reference_id uuid,
    reversal_of_movement_id uuid,
    reason varchar(500),
    confirmed_at timestamptz DEFAULT now() NOT NULL,
    confirmed_by uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_movements_warehouse FOREIGN KEY (warehouse_id, tenant_id) REFERENCES warehouses (id, tenant_id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id, tenant_id) REFERENCES products (id, tenant_id),
    CONSTRAINT fk_stock_movements_reversal FOREIGN KEY (reversal_of_movement_id, tenant_id) REFERENCES stock_movements (id, tenant_id),
    CONSTRAINT fk_stock_movements_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES user_accounts (id),
    CONSTRAINT uq_stock_movements_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity > 0),
    CONSTRAINT ck_stock_movements_unit_cost CHECK (unit_cost IS NULL OR unit_cost >= 0),
    CONSTRAINT ck_stock_movements_currency_code CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_stock_movements_tenant_product_date ON stock_movements (tenant_id, product_id, confirmed_at DESC);
CREATE INDEX idx_stock_movements_tenant_warehouse_date ON stock_movements (tenant_id, warehouse_id, confirmed_at DESC);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id) WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;

CREATE TABLE audit_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid,
    actor_user_id uuid,
    actor_type varchar(32) DEFAULT 'SYSTEM' NOT NULL,
    action varchar(120) NOT NULL,
    resource_type varchar(120) NOT NULL,
    resource_id uuid,
    outcome varchar(32) DEFAULT 'SUCCESS' NOT NULL,
    source_ip inet,
    user_agent varchar(512),
    trace_id varchar(128),
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    occurred_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id),
    CONSTRAINT fk_audit_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES user_accounts (id),
    CONSTRAINT ck_audit_events_actor_type CHECK (actor_type IN ('USER', 'SYSTEM')),
    CONSTRAINT ck_audit_events_actor_consistency CHECK ((actor_type = 'USER' AND actor_user_id IS NOT NULL) OR actor_type = 'SYSTEM'),
    CONSTRAINT ck_audit_events_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_audit_events_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX idx_audit_events_tenant_occurred_at ON audit_events (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_events_actor_occurred_at ON audit_events (actor_user_id, occurred_at DESC) WHERE actor_user_id IS NOT NULL;
CREATE INDEX idx_audit_events_resource ON audit_events (resource_type, resource_id) WHERE resource_id IS NOT NULL;
CREATE INDEX idx_audit_events_trace_id ON audit_events (trace_id) WHERE trace_id IS NOT NULL;