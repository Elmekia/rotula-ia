-- V6: Campos nutricionales por 100 g en ingredientes + tamaño de porción y
--      contaminación cruzada en productos.
--
-- Todos los campos nutricionales son NULLABLE: no es obligatorio cargar esta
-- información para cada ingrediente. El cálculo de tabla nutricional del producto
-- solo se realiza cuando al menos un ingrediente tiene datos nutricionales.

-- ── Ingredientes: info nutricional por 100 g del ingrediente ─────────────────
ALTER TABLE ingredients
    ADD COLUMN energy_kcal_per100g  NUMERIC(8,2),
    ADD COLUMN proteins_g_per100g   NUMERIC(8,2),
    ADD COLUMN carbs_g_per100g      NUMERIC(8,2),
    ADD COLUMN sugars_g_per100g     NUMERIC(8,2),
    ADD COLUMN fat_total_g_per100g  NUMERIC(8,2),
    ADD COLUMN fat_sat_g_per100g    NUMERIC(8,2),
    ADD COLUMN fat_trans_g_per100g  NUMERIC(8,2),
    ADD COLUMN sodium_mg_per100g    NUMERIC(8,2);

-- ── Productos: porción de referencia y contaminación cruzada declarada ────────
ALTER TABLE products
    ADD COLUMN serving_size_g      NUMERIC(8,2),           -- en gramos o mL
    ADD COLUMN cross_contamination TEXT;                    -- nombres de grupos alérgenos separados por coma
