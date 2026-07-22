-- Sprint 4 Hito 7: stable operational audit pagination.
-- Supports tenant-scoped keyset pagination ordered by occurred_at DESC, id DESC.

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_occurred_at_id
    ON audit_events (tenant_id, occurred_at DESC, id DESC);
