CREATE TABLE user_accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email citext NOT NULL,
    display_name varchar(160) NOT NULL,
    password_hash varchar(255),
    status varchar(32) DEFAULT 'INVITED' NOT NULL,
    locale varchar(16),
    time_zone varchar(64),
    last_login_at timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT ck_user_accounts_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_user_accounts_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_user_accounts_email_active ON user_accounts (email) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_accounts_status ON user_accounts (status) WHERE deleted_at IS NULL;

CREATE TABLE permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code varchar(120) NOT NULL,
    resource varchar(80) NOT NULL,
    action varchar(40) NOT NULL,
    description varchar(240),
    is_system boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code),
    CONSTRAINT uq_permissions_resource_action UNIQUE (resource, action),
    CONSTRAINT ck_permissions_code_format CHECK (code = resource || ':' || action)
);

CREATE TABLE roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid,
    code varchar(80) NOT NULL,
    name varchar(160) NOT NULL,
    scope varchar(16) NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    is_assignable boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_roles_id_scope UNIQUE (id, scope),
    CONSTRAINT uq_roles_id_tenant_scope UNIQUE (id, tenant_id, scope),
    CONSTRAINT ck_roles_scope CHECK (scope IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_roles_scope_tenant CHECK ((scope = 'PLATFORM' AND tenant_id IS NULL) OR (scope = 'TENANT' AND tenant_id IS NOT NULL)),
    CONSTRAINT ck_roles_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_roles_platform_code_active ON roles (lower(code)) WHERE scope = 'PLATFORM' AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_roles_tenant_code_active ON roles (tenant_id, lower(code)) WHERE scope = 'TENANT' AND deleted_at IS NULL;
CREATE INDEX idx_roles_tenant_scope ON roles (tenant_id, scope) WHERE deleted_at IS NULL;

CREATE TABLE role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE tenant_memberships (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    status varchar(32) DEFAULT 'INVITED' NOT NULL,
    joined_at timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz DEFAULT now() NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT pk_tenant_memberships PRIMARY KEY (id),
    CONSTRAINT fk_tenant_memberships_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_memberships_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT uq_tenant_memberships_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_tenant_memberships_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
    CONSTRAINT ck_tenant_memberships_deleted_at_range CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uq_tenant_memberships_tenant_user_active ON tenant_memberships (tenant_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenant_memberships_user ON tenant_memberships (user_id) WHERE deleted_at IS NULL;

CREATE TABLE tenant_membership_roles (
    tenant_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    role_id uuid NOT NULL,
    role_scope varchar(16) DEFAULT 'TENANT' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_tenant_membership_roles PRIMARY KEY (membership_id, role_id),
    CONSTRAINT fk_tenant_membership_roles_membership FOREIGN KEY (membership_id, tenant_id) REFERENCES tenant_memberships (id, tenant_id),
    CONSTRAINT fk_tenant_membership_roles_role FOREIGN KEY (role_id, tenant_id, role_scope) REFERENCES roles (id, tenant_id, scope),
    CONSTRAINT ck_tenant_membership_roles_scope CHECK (role_scope = 'TENANT')
);

CREATE INDEX idx_tenant_membership_roles_tenant ON tenant_membership_roles (tenant_id);

CREATE TABLE platform_role_assignments (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    role_scope varchar(16) DEFAULT 'PLATFORM' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    CONSTRAINT pk_platform_role_assignments PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_platform_role_assignments_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_platform_role_assignments_role FOREIGN KEY (role_id, role_scope) REFERENCES roles (id, scope),
    CONSTRAINT ck_platform_role_assignments_scope CHECK (role_scope = 'PLATFORM')
);

CREATE TABLE refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    tenant_id uuid,
    token_hash varchar(160) NOT NULL,
    issued_at timestamptz DEFAULT now() NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    replaced_by_token_id uuid,
    source_ip inet,
    user_agent varchar(512),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_refresh_tokens_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_expiration CHECK (expires_at > issued_at),
    CONSTRAINT ck_refresh_tokens_revocation CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_tenant ON refresh_tokens (tenant_id) WHERE tenant_id IS NOT NULL;