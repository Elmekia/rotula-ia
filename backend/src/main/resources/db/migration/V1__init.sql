-- ══════════════════════════════════════════════════════════
--  RotulaIA — Schema inicial
--  V1__init.sql
-- ══════════════════════════════════════════════════════════

-- Extensiones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Tenants ────────────────────────────────────────────────
CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(200) NOT NULL,
    cuit       VARCHAR(13),
    rne_number VARCHAR(20),
    plan       VARCHAR(20) NOT NULL DEFAULT 'starter'
                CHECK (plan IN ('starter','pro','business','enterprise')),
    status     VARCHAR(20) NOT NULL DEFAULT 'active'
                CHECK (status IN ('active','suspended','cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Users ──────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200),
    role          VARCHAR(20) NOT NULL DEFAULT 'user'
                   CHECK (role IN ('owner','admin','user','readonly')),
    status        VARCHAR(20) NOT NULL DEFAULT 'active'
                   CHECK (status IN ('active','inactive','pending')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Products ───────────────────────────────────────────────
CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(300) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    net_weight  NUMERIC(10,3) NOT NULL,
    weight_unit VARCHAR(10) NOT NULL DEFAULT 'g'
                 CHECK (weight_unit IN ('g','kg','ml','l','u')),
    rne_number  VARCHAR(20),
    rnpa_number VARCHAR(20),
    status      VARCHAR(20) NOT NULL DEFAULT 'active',
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Ingredients ────────────────────────────────────────────
CREATE TABLE ingredients (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id   UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tenant_id    UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name         VARCHAR(300) NOT NULL,
    percentage   NUMERIC(6,3),
    is_allergen  BOOLEAN NOT NULL DEFAULT false,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Label Projects ─────────────────────────────────────────
CREATE TABLE label_projects (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id          UUID NOT NULL REFERENCES products(id),
    created_by          UUID REFERENCES users(id),
    version             INTEGER NOT NULL DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'draft'
                         CHECK (status IN ('draft','approved','exported')),
    legal_denomination  VARCHAR(500),
    manufacturer_info   JSONB,
    disclaimer_accepted BOOLEAN NOT NULL DEFAULT false,
    disclaimer_at       TIMESTAMPTZ,
    disclaimer_by       UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Nutrition Tables ───────────────────────────────────────
CREATE TABLE nutrition_tables (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label_id         UUID NOT NULL REFERENCES label_projects(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    serving_size_g   NUMERIC(8,2) NOT NULL,
    energy_kcal      NUMERIC(8,2),
    energy_kj        NUMERIC(8,2),
    proteins_g       NUMERIC(8,2),
    carbs_g          NUMERIC(8,2),
    sugars_g         NUMERIC(8,2),
    fat_total_g      NUMERIC(8,2),
    fat_saturated_g  NUMERIC(8,2),
    fat_trans_g      NUMERIC(8,2),
    sodium_mg        NUMERIC(8,2),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Allergen Warnings ──────────────────────────────────────
CREATE TABLE allergen_warnings (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label_id         UUID NOT NULL REFERENCES label_projects(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    allergen_type    VARCHAR(50) NOT NULL,
    declaration_text TEXT NOT NULL,
    is_mandatory     BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Regulatory Seals ───────────────────────────────────────
CREATE TABLE regulatory_seals (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label_id         UUID NOT NULL REFERENCES label_projects(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    seal_type        VARCHAR(50) NOT NULL,
    regulation_ref   VARCHAR(100),
    required         BOOLEAN NOT NULL DEFAULT false,
    applied          BOOLEAN NOT NULL DEFAULT false,
    justification    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Audit Log ──────────────────────────────────────────────
CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL,
    user_id     UUID,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID NOT NULL,
    action      VARCHAR(50) NOT NULL,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Índices ────────────────────────────────────────────────
CREATE INDEX idx_users_tenant        ON users(tenant_id);
CREATE INDEX idx_products_tenant     ON products(tenant_id);
CREATE INDEX idx_ingredients_product ON ingredients(product_id);
CREATE INDEX idx_labels_tenant       ON label_projects(tenant_id);
CREATE INDEX idx_labels_product      ON label_projects(product_id);
CREATE INDEX idx_audit_tenant        ON audit_log(tenant_id);
CREATE INDEX idx_audit_entity        ON audit_log(entity_type, entity_id);

-- ── Row Level Security ─────────────────────────────────────
ALTER TABLE products          ENABLE ROW LEVEL SECURITY;
ALTER TABLE ingredients       ENABLE ROW LEVEL SECURITY;
ALTER TABLE label_projects    ENABLE ROW LEVEL SECURITY;
ALTER TABLE nutrition_tables  ENABLE ROW LEVEL SECURITY;
ALTER TABLE allergen_warnings ENABLE ROW LEVEL SECURITY;
ALTER TABLE regulatory_seals  ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON products
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation ON ingredients
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation ON label_projects
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation ON nutrition_tables
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation ON allergen_warnings
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation ON regulatory_seals
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
