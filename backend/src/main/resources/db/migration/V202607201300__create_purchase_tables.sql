CREATE TABLE purchase_number_sequences (
    tenant_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    next_number bigint DEFAULT 1 NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_purchase_number_sequences PRIMARY KEY (tenant_id, branch_id),
    CONSTRAINT fk_purchase_number_sequences_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_purchase_number_sequences_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT ck_purchase_number_sequences_next_number CHECK (next_number > 0)
);

CREATE TABLE purchase_orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    purchase_number bigint NOT NULL,
    number_code varchar(40) NOT NULL,
    supplier_document_number varchar(120),
    status varchar(32) DEFAULT 'DRAFT' NOT NULL,
    currency_code char(3) NOT NULL,
    subtotal_amount numeric(19, 4) DEFAULT 0 NOT NULL,
    total_amount numeric(19, 4) DEFAULT 0 NOT NULL,
    payment_status varchar(32) DEFAULT 'UNPAID' NOT NULL,
    payment_method varchar(32),
    paid_amount numeric(19, 4) DEFAULT 0 NOT NULL,
    cash_session_id uuid,
    cash_movement_id uuid,
    payment_reversal_cash_session_id uuid,
    payment_reversal_cash_movement_id uuid,
    confirmed_at timestamptz,
    confirmed_by uuid,
    cancelled_at timestamptz,
    cancelled_by uuid,
    cancellation_reason varchar(500),
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_purchase_orders PRIMARY KEY (id),
    CONSTRAINT fk_purchase_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_purchase_orders_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id, tenant_id) REFERENCES business_partners (id, tenant_id),
    CONSTRAINT fk_purchase_orders_cash_session FOREIGN KEY (cash_session_id, tenant_id) REFERENCES cash_sessions (id, tenant_id),
    CONSTRAINT fk_purchase_orders_cash_movement FOREIGN KEY (cash_movement_id, tenant_id) REFERENCES cash_movements (id, tenant_id),
    CONSTRAINT fk_purchase_orders_reversal_cash_session FOREIGN KEY (payment_reversal_cash_session_id, tenant_id) REFERENCES cash_sessions (id, tenant_id),
    CONSTRAINT fk_purchase_orders_reversal_cash_movement FOREIGN KEY (payment_reversal_cash_movement_id, tenant_id) REFERENCES cash_movements (id, tenant_id),
    CONSTRAINT fk_purchase_orders_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES user_accounts (id),
    CONSTRAINT fk_purchase_orders_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES user_accounts (id),
    CONSTRAINT uq_purchase_orders_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_purchase_orders_number UNIQUE (tenant_id, branch_id, purchase_number),
    CONSTRAINT ck_purchase_orders_number_positive CHECK (purchase_number > 0),
    CONSTRAINT ck_purchase_orders_number_code_not_blank CHECK (length(trim(number_code)) > 0),
    CONSTRAINT ck_purchase_orders_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_purchase_orders_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_purchase_orders_amounts CHECK (subtotal_amount >= 0 AND total_amount >= 0 AND paid_amount >= 0),
    CONSTRAINT ck_purchase_orders_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'REVERSED')),
    CONSTRAINT ck_purchase_orders_payment_method CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER')),
    CONSTRAINT ck_purchase_orders_payment_pair CHECK ((payment_method IS NULL AND cash_session_id IS NULL AND cash_movement_id IS NULL AND paid_amount = 0) OR (payment_method IS NOT NULL AND cash_session_id IS NOT NULL AND cash_movement_id IS NOT NULL AND paid_amount > 0)),
    CONSTRAINT ck_purchase_orders_payment_status_consistency CHECK (
        (payment_status = 'UNPAID' AND payment_method IS NULL AND paid_amount = 0)
        OR (payment_status = 'PAID' AND payment_method IS NOT NULL AND paid_amount = total_amount)
        OR (payment_status = 'REVERSED' AND payment_method IS NOT NULL AND paid_amount = total_amount AND payment_reversal_cash_session_id IS NOT NULL AND payment_reversal_cash_movement_id IS NOT NULL)
    ),
    CONSTRAINT ck_purchase_orders_confirmed_fields CHECK (
        (status = 'DRAFT' AND confirmed_at IS NULL AND confirmed_by IS NULL AND cancelled_at IS NULL AND cancelled_by IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL AND cancelled_at IS NULL AND cancelled_by IS NULL)
        OR (status = 'CANCELLED' AND confirmed_at IS NOT NULL AND confirmed_by IS NOT NULL AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    ),
    CONSTRAINT ck_purchase_orders_deleted_draft CHECK (deleted_at IS NULL OR status = 'DRAFT'),
    CONSTRAINT ck_purchase_orders_cancelled_at_range CHECK (cancelled_at IS NULL OR cancelled_at >= confirmed_at),
    CONSTRAINT ck_purchase_orders_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX idx_purchase_orders_tenant_status_date ON purchase_orders (tenant_id, status, updated_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_tenant_supplier_date ON purchase_orders (tenant_id, supplier_id, updated_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_tenant_branch_number ON purchase_orders (tenant_id, branch_id, purchase_number DESC) WHERE deleted_at IS NULL;

CREATE TABLE purchase_order_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    purchase_id uuid NOT NULL,
    line_number integer NOT NULL,
    product_id uuid NOT NULL,
    warehouse_id uuid,
    product_sku varchar(80) NOT NULL,
    product_name varchar(220) NOT NULL,
    product_type varchar(32) NOT NULL,
    stock_tracking_enabled boolean DEFAULT false NOT NULL,
    quantity numeric(19, 6) NOT NULL,
    unit_cost numeric(19, 4) NOT NULL,
    subtotal_amount numeric(19, 4) NOT NULL,
    stock_movement_id uuid,
    stock_reversal_movement_id uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_purchase_order_items PRIMARY KEY (id),
    CONSTRAINT fk_purchase_order_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_purchase_order_items_purchase FOREIGN KEY (purchase_id, tenant_id) REFERENCES purchase_orders (id, tenant_id),
    CONSTRAINT fk_purchase_order_items_product FOREIGN KEY (product_id, tenant_id) REFERENCES products (id, tenant_id),
    CONSTRAINT fk_purchase_order_items_warehouse FOREIGN KEY (warehouse_id, tenant_id) REFERENCES warehouses (id, tenant_id),
    CONSTRAINT fk_purchase_order_items_stock_movement FOREIGN KEY (stock_movement_id, tenant_id) REFERENCES stock_movements (id, tenant_id),
    CONSTRAINT fk_purchase_order_items_stock_reversal FOREIGN KEY (stock_reversal_movement_id, tenant_id) REFERENCES stock_movements (id, tenant_id),
    CONSTRAINT uq_purchase_order_items_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_purchase_order_items_line UNIQUE (tenant_id, purchase_id, line_number),
    CONSTRAINT ck_purchase_order_items_line_positive CHECK (line_number > 0),
    CONSTRAINT ck_purchase_order_items_product_snapshot_not_blank CHECK (length(trim(product_sku)) > 0 AND length(trim(product_name)) > 0),
    CONSTRAINT ck_purchase_order_items_product_type CHECK (product_type = 'GOOD'),
    CONSTRAINT ck_purchase_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_purchase_order_items_amounts CHECK (unit_cost >= 0 AND subtotal_amount >= 0),
    CONSTRAINT ck_purchase_order_items_stock_tracking_warehouse CHECK (stock_tracking_enabled = false OR warehouse_id IS NOT NULL)
);

CREATE INDEX idx_purchase_order_items_tenant_product ON purchase_order_items (tenant_id, product_id);
CREATE INDEX idx_purchase_order_items_tenant_purchase ON purchase_order_items (tenant_id, purchase_id, line_number);