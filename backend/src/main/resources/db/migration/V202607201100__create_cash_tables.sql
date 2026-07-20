CREATE TABLE cash_registers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_cash_registers PRIMARY KEY (id),
    CONSTRAINT fk_cash_registers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_cash_registers_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT uq_cash_registers_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_cash_registers_code_not_blank CHECK (length(trim(code)) > 0),
    CONSTRAINT ck_cash_registers_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_cash_registers_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_cash_registers_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_cash_registers_tenant_branch_code_active
    ON cash_registers (tenant_id, branch_id, lower(code))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cash_registers_tenant_status ON cash_registers (tenant_id, status) WHERE deleted_at IS NULL;

CREATE TABLE cash_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    cash_register_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    opened_by_user_id uuid NOT NULL,
    status varchar(32) DEFAULT 'OPEN' NOT NULL,
    opening_amount numeric(19, 4) DEFAULT 0 NOT NULL,
    currency_code char(3) NOT NULL,
    expected_closing_amount numeric(19, 4),
    counted_closing_amount numeric(19, 4),
    closing_difference numeric(19, 4),
    opened_at timestamptz DEFAULT now() NOT NULL,
    closed_at timestamptz,
    closed_by_user_id uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_cash_sessions PRIMARY KEY (id),
    CONSTRAINT fk_cash_sessions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_cash_sessions_register FOREIGN KEY (cash_register_id, tenant_id) REFERENCES cash_registers (id, tenant_id),
    CONSTRAINT fk_cash_sessions_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT fk_cash_sessions_opened_by FOREIGN KEY (opened_by_user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_cash_sessions_closed_by FOREIGN KEY (closed_by_user_id) REFERENCES user_accounts (id),
    CONSTRAINT uq_cash_sessions_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_cash_sessions_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_cash_sessions_opening_amount CHECK (opening_amount >= 0),
    CONSTRAINT ck_cash_sessions_counted_amount CHECK (counted_closing_amount IS NULL OR counted_closing_amount >= 0),
    CONSTRAINT ck_cash_sessions_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_cash_sessions_closed_fields CHECK (
        (status = 'OPEN' AND closed_at IS NULL AND closed_by_user_id IS NULL AND expected_closing_amount IS NULL AND counted_closing_amount IS NULL AND closing_difference IS NULL)
        OR
        (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by_user_id IS NOT NULL AND expected_closing_amount IS NOT NULL AND counted_closing_amount IS NOT NULL AND closing_difference IS NOT NULL)
    ),
    CONSTRAINT ck_cash_sessions_closed_at_range CHECK (closed_at IS NULL OR closed_at >= opened_at)
);

CREATE UNIQUE INDEX uq_cash_sessions_open_by_register_user
    ON cash_sessions (tenant_id, cash_register_id, opened_by_user_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_cash_sessions_tenant_status ON cash_sessions (tenant_id, status, opened_at DESC);
CREATE INDEX idx_cash_sessions_tenant_user ON cash_sessions (tenant_id, opened_by_user_id, opened_at DESC);

CREATE TABLE cash_movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    cash_session_id uuid NOT NULL,
    cash_register_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    movement_type varchar(32) NOT NULL,
    payment_method varchar(32) NOT NULL,
    amount numeric(19, 4) NOT NULL,
    cash_effect numeric(19, 4) NOT NULL,
    currency_code char(3) NOT NULL,
    reference_type varchar(80),
    reference_id uuid,
    description varchar(500),
    occurred_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_cash_movements PRIMARY KEY (id),
    CONSTRAINT fk_cash_movements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_cash_movements_session FOREIGN KEY (cash_session_id, tenant_id) REFERENCES cash_sessions (id, tenant_id),
    CONSTRAINT fk_cash_movements_register FOREIGN KEY (cash_register_id, tenant_id) REFERENCES cash_registers (id, tenant_id),
    CONSTRAINT fk_cash_movements_branch FOREIGN KEY (branch_id, tenant_id) REFERENCES branches (id, tenant_id),
    CONSTRAINT uq_cash_movements_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_cash_movements_type CHECK (movement_type IN ('OPENING', 'SALE_PAYMENT', 'PURCHASE_PAYMENT', 'CASH_IN', 'CASH_OUT', 'CLOSING_ADJUSTMENT')),
    CONSTRAINT ck_cash_movements_payment_method CHECK (payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER')),
    CONSTRAINT ck_cash_movements_amount CHECK (amount >= 0),
    CONSTRAINT ck_cash_movements_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_cash_movements_reference_pair CHECK ((reference_type IS NULL AND reference_id IS NULL) OR (reference_type IS NOT NULL AND reference_id IS NOT NULL))
);

CREATE INDEX idx_cash_movements_tenant_session ON cash_movements (tenant_id, cash_session_id, occurred_at DESC);
CREATE INDEX idx_cash_movements_tenant_type ON cash_movements (tenant_id, movement_type, occurred_at DESC);
CREATE INDEX idx_cash_movements_reference ON cash_movements (reference_type, reference_id) WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;