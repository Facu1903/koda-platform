-- Sprint 4 Hito 5: query-backed operational indexes.
-- Every index below maps to an existing repository query. Avoid adding indexes
-- without a concrete read path because each one increases write cost.

CREATE INDEX IF NOT EXISTS idx_tenant_product_subscriptions_active_validity
    ON tenant_product_subscriptions (tenant_id, product_id, valid_from, valid_until)
    INCLUDE (plan_id)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_tenant_feature_flags_effective
    ON tenant_feature_flags (tenant_id, product_id, module_id, valid_from, valid_until);

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_actor_occurred_at
    ON audit_events (tenant_id, actor_user_id, occurred_at DESC, id DESC)
    WHERE actor_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_resource_occurred_at
    ON audit_events (tenant_id, resource_type, resource_id, occurred_at DESC, id DESC)
    WHERE resource_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_action_occurred_at
    ON audit_events (tenant_id, action, occurred_at DESC, id DESC);

DROP INDEX IF EXISTS idx_sales_orders_tenant_confirmed_at;

CREATE INDEX idx_sales_orders_tenant_confirmed_at
    ON sales_orders (tenant_id, confirmed_at DESC, sale_number DESC)
    WHERE deleted_at IS NULL AND status IN ('CONFIRMED', 'CANCELLED');

DROP INDEX IF EXISTS idx_purchase_orders_tenant_confirmed_at;

CREATE INDEX idx_purchase_orders_tenant_confirmed_at
    ON purchase_orders (tenant_id, confirmed_at DESC, purchase_number DESC)
    WHERE deleted_at IS NULL AND status IN ('CONFIRMED', 'CANCELLED');

DROP INDEX IF EXISTS idx_stock_movements_tenant_confirmed_at;

CREATE INDEX idx_stock_movements_tenant_confirmed_at
    ON stock_movements (tenant_id, confirmed_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_updated_at
    ON sales_orders (tenant_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_tenant_updated_at
    ON purchase_orders (tenant_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_stock_balances_tenant_updated_at
    ON stock_balances (tenant_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_cash_sessions_tenant_user_open
    ON cash_sessions (tenant_id, opened_by_user_id, opened_at DESC)
    WHERE status = 'OPEN';

