-- ── RLS en tabla users ────────────────────────────────────────
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- ── FORCE RLS en tablas existentes ────────────────────────────
-- Garantiza aislamiento incluso para el owner de la tabla / superusuario
ALTER TABLE products          FORCE ROW LEVEL SECURITY;
ALTER TABLE ingredients       FORCE ROW LEVEL SECURITY;
ALTER TABLE label_projects    FORCE ROW LEVEL SECURITY;
ALTER TABLE nutrition_tables  FORCE ROW LEVEL SECURITY;
ALTER TABLE allergen_warnings FORCE ROW LEVEL SECURITY;
ALTER TABLE regulatory_seals  FORCE ROW LEVEL SECURITY;
