-- ══════════════════════════════════════════════════════════
--  V3__weight_unit_add_cc.sql
--  Amplía el CHECK de weight_unit para aceptar 'cc'
--  (centímetros cúbicos, habitual en etiquetas alimentarias ARG).
--  El constraint original de V1 solo permitía ('g','kg','ml','l','u').
-- ══════════════════════════════════════════════════════════

DO $$
DECLARE
    v_constraint text;
BEGIN
    -- Busca el CHECK constraint que menciona 'weight_unit' en la tabla products
    SELECT conname INTO v_constraint
    FROM pg_constraint
    WHERE conrelid = 'products'::regclass
      AND contype  = 'c'
      AND pg_get_constraintdef(oid) LIKE '%weight_unit%'
    LIMIT 1;

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE products DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
END $$;

ALTER TABLE products
    ADD CONSTRAINT products_weight_unit_check
    CHECK (weight_unit IN ('g', 'kg', 'ml', 'l', 'u', 'cc'));
