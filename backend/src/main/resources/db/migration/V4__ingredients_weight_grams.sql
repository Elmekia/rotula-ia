-- ══════════════════════════════════════════════════════════
--  V4: Reemplaza percentage por weight_grams en ingredients
--      El % se calcula dinámicamente como weight / sum(weights) * 100
--      y el orden queda implícito al ordenar por weight_grams DESC.
-- ══════════════════════════════════════════════════════════

-- Renombrar y ampliar precisión (de NUMERIC(6,3) a NUMERIC(10,3))
ALTER TABLE ingredients RENAME COLUMN percentage TO weight_grams;
ALTER TABLE ingredients ALTER COLUMN weight_grams TYPE NUMERIC(10,3);

-- Garantizar NOT NULL (rellenar cualquier NULL previo con un valor mínimo)
UPDATE ingredients SET weight_grams = 0.001 WHERE weight_grams IS NULL;
ALTER TABLE ingredients ALTER COLUMN weight_grams SET NOT NULL;

-- Eliminar sort_order (ahora el orden lo da weight_grams DESC)
ALTER TABLE ingredients DROP COLUMN IF EXISTS sort_order;
