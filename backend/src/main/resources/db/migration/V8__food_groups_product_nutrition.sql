-- ══════════════════════════════════════════════════════════════════════════
--  V8: Grupos y alimentos de referencia (TABLA I – Res. Conjunta 21/2023)
--      + Reestructuración del producto:
--          • denomination (denominación del alimento)
--          • food_group_id / food_item_id  (reemplaza category)
--          • rnpa_number NOT NULL
--          • show_ingredient_percentages
--          • tabla nutricional por 100 g en el producto
--      + Simplificación de ingredientes:
--          • se eliminan los campos nutricionales por 100 g
--
--  Fuente de porciones:
--    Res. Conjunta MSAL / MAGyP / MTYSS 21/2023, Anexo I – TABLA I
--    (Porciones de referencia por grupo de alimentos para etiquetado
--     nutricional conforme Ley 27.642 / Decreto 151/2022)
-- ══════════════════════════════════════════════════════════════════════════


-- ── 1. Tabla de grupos de alimentos ──────────────────────────────────────

CREATE TABLE food_groups (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    code        VARCHAR(10) NOT NULL UNIQUE,   -- ej. G1, G2 … G8
    name        TEXT        NOT NULL,
    sort_order  SMALLINT    NOT NULL DEFAULT 0,
    active      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ── 2. Tabla de alimentos de referencia ──────────────────────────────────

CREATE TABLE food_items (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    food_group_id   UUID         NOT NULL REFERENCES food_groups(id),
    name            TEXT         NOT NULL,
    portion_grams   NUMERIC(8,2) NOT NULL,   -- porción de referencia en g o mL
    unit            VARCHAR(5)   NOT NULL DEFAULT 'g',  -- 'g' o 'ml'
    sort_order      SMALLINT     NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_food_items_group ON food_items(food_group_id);


-- ══════════════════════════════════════════════════════════════════════════
--  SEED — TABLA I (Res. Conjunta 21/2023 / Ley 27.642)
-- ══════════════════════════════════════════════════════════════════════════

-- ── Grupo 1: Panificación, cereales, leguminosas, raíces y tubérculos ────

INSERT INTO food_groups (code, name, sort_order) VALUES
('G1', 'Productos de panificación, cereales, leguminosas, raíces, tubérculos y sus derivados', 1),
('G2', 'Verduras y frutas',                                                                    2),
('G3', 'Lácteos',                                                                              3),
('G4', 'Carnes y huevos',                                                                      4),
('G5', 'Aceites, grasas y semillas',                                                           5),
('G6', 'Azúcares y dulces',                                                                    6),
('G7', 'Bebidas no alcohólicas',                                                               7),
('G8', 'Varios (aderezos, sopas, caldos, salsas)',                                             8);

-- G1
WITH g AS (SELECT id FROM food_groups WHERE code = 'G1')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Pan (francés, flauta, lactal, de molde)',        50,    'g', 1),
((SELECT id FROM g), 'Facturas, medialunas, croissants',               50,    'g', 2),
((SELECT id FROM g), 'Galletitas (dulces o saladas)',                  30,    'g', 3),
((SELECT id FROM g), 'Bizcochos, tostadas',                            30,    'g', 4),
((SELECT id FROM g), 'Cereales para desayuno (copos, muesli)',         30,    'g', 5),
((SELECT id FROM g), 'Fideos y pastas secas (sin cocinar)',            80,    'g', 6),
((SELECT id FROM g), 'Fideos y pastas frescas (sin cocinar)',          100,   'g', 7),
((SELECT id FROM g), 'Arroz (sin cocinar)',                            30,    'g', 8),
((SELECT id FROM g), 'Harina de trigo / maíz / otros cereales',       30,    'g', 9),
((SELECT id FROM g), 'Avena en hojuelas',                              30,    'g', 10),
((SELECT id FROM g), 'Legumbres secas (porotos, lentejas, garbanzos)', 30,   'g', 11),
((SELECT id FROM g), 'Soja en grano / proteína de soja texturizada',  30,    'g', 12),
((SELECT id FROM g), 'Papa, batata, mandioca (frescas o congeladas)', 150,   'g', 13),
((SELECT id FROM g), 'Puré de papa instantáneo (sin preparar)',       25,    'g', 14),
((SELECT id FROM g), 'Polenta / sémola de maíz (sin cocinar)',        30,    'g', 15);

-- G2
WITH g AS (SELECT id FROM food_groups WHERE code = 'G2')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Verduras frescas o congeladas (en general)',     60,   'g', 1),
((SELECT id FROM g), 'Verduras de hoja (lechuga, espinaca, rúcula)',   30,   'g', 2),
((SELECT id FROM g), 'Tomate fresco',                                  80,   'g', 3),
((SELECT id FROM g), 'Frutas frescas (en general)',                    100,  'g', 4),
((SELECT id FROM g), 'Frutas secas / deshidratadas',                  30,   'g', 5),
((SELECT id FROM g), 'Jugos y purés 100% fruta',                      200,  'ml', 6);

-- G3
WITH g AS (SELECT id FROM food_groups WHERE code = 'G3')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Leche fluida (entera, semidescremada, descremada)', 200, 'ml', 1),
((SELECT id FROM g), 'Leche en polvo',                                     25,  'g',  2),
((SELECT id FROM g), 'Yogur (natural, con frutas, bebible)',               200, 'g',  3),
((SELECT id FROM g), 'Leche fermentada / kéfir',                          200, 'ml', 4),
((SELECT id FROM g), 'Queso duro o semiduro (reggianito, fontina, etc.)',  30,  'g',  5),
((SELECT id FROM g), 'Queso blando o cremoso (ricota, cottage, queso crema)', 100, 'g', 6),
((SELECT id FROM g), 'Dulce de leche',                                    30,  'g',  7),
((SELECT id FROM g), 'Helado base láctea',                                100, 'g',  8);

-- G4
WITH g AS (SELECT id FROM food_groups WHERE code = 'G4')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Carne vacuna (cortes varios)',              100,  'g', 1),
((SELECT id FROM g), 'Carne de cerdo',                            100,  'g', 2),
((SELECT id FROM g), 'Pollo (pechuga, muslo, pata, ala)',         100,  'g', 3),
((SELECT id FROM g), 'Pescado (filetes, postas)',                 100,  'g', 4),
((SELECT id FROM g), 'Mariscos (camarones, mejillones, calamar)', 100, 'g', 5),
((SELECT id FROM g), 'Huevo entero',                              60,   'g', 6),
((SELECT id FROM g), 'Fiambres cocidos (jamón, paleta, peceto)',  30,   'g', 7),
((SELECT id FROM g), 'Embutidos (salchicha, chorizo, salame)',    30,   'g', 8),
((SELECT id FROM g), 'Hamburguesa',                               100,  'g', 9),
((SELECT id FROM g), 'Patés y untables cárnicos',                30,   'g', 10);

-- G5
WITH g AS (SELECT id FROM food_groups WHERE code = 'G5')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Aceite vegetal (girasol, maíz, soja, oliva, etc.)', 10, 'ml', 1),
((SELECT id FROM g), 'Manteca o margarina',                               10,  'g',  2),
((SELECT id FROM g), 'Crema de leche',                                    30,  'g',  3),
((SELECT id FROM g), 'Semillas (sésamo, chía, lino, girasol, zapallo)',   30,  'g',  4),
((SELECT id FROM g), 'Maní (tostado, con o sin sal)',                     30,  'g',  5),
((SELECT id FROM g), 'Frutos secos (almendras, nueces, avellanas, etc.)', 30,  'g',  6),
((SELECT id FROM g), 'Pasta de maní / mantequilla de almendras',          30,  'g',  7);

-- G6
WITH g AS (SELECT id FROM food_groups WHERE code = 'G6')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Azúcar (blanca, negra, mascabo)',    10, 'g', 1),
((SELECT id FROM g), 'Miel',                               10, 'g', 2),
((SELECT id FROM g), 'Mermelada o jalea',                  20, 'g', 3),
((SELECT id FROM g), 'Dulce de batata / membrillo',        30, 'g', 4),
((SELECT id FROM g), 'Chocolate (tableta, cobertura)',     30, 'g', 5),
((SELECT id FROM g), 'Cacao en polvo',                     10, 'g', 6),
((SELECT id FROM g), 'Caramelos, gomitas, golosinas',      30, 'g', 7),
((SELECT id FROM g), 'Facturas y repostería sin relleno',  50, 'g', 8);

-- G7
WITH g AS (SELECT id FROM food_groups WHERE code = 'G7')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Agua (sola o mineral)',                            200, 'ml', 1),
((SELECT id FROM g), 'Bebida gaseosa o soda',                            200, 'ml', 2),
((SELECT id FROM g), 'Jugo de frutas (100% jugo, néctar o bebida)',     200, 'ml', 3),
((SELECT id FROM g), 'Bebida saborizada / agua saborizada',              200, 'ml', 4),
((SELECT id FROM g), 'Bebida energizante',                               250, 'ml', 5),
((SELECT id FROM g), 'Isotónica / bebida deportiva',                     200, 'ml', 6),
((SELECT id FROM g), 'Bebida en polvo (preparada)',                      200, 'ml', 7),
((SELECT id FROM g), 'Infusión (té, mate, café, tilo, etc.)',            200, 'ml', 8),
((SELECT id FROM g), 'Caldo líquido listo para consumir',                200, 'ml', 9);

-- G8
WITH g AS (SELECT id FROM food_groups WHERE code = 'G8')
INSERT INTO food_items (food_group_id, name, portion_grams, unit, sort_order) VALUES
((SELECT id FROM g), 'Aderezo líquido (mayonesa, kétchup, mostaza)',     15, 'g',  1),
((SELECT id FROM g), 'Aderezo en sachet / sobre individual',             10, 'g',  2),
((SELECT id FROM g), 'Salsa (tomate, salsa blanca, fileto, etc.)',       60, 'g',  3),
((SELECT id FROM g), 'Sal (fina, gruesa, marina)',                        5, 'g',  4),
((SELECT id FROM g), 'Especias y hierbas secas',                          3, 'g',  5),
((SELECT id FROM g), 'Caldo concentrado en cubito o polvo',               5, 'g',  6),
((SELECT id FROM g), 'Sopa en sobre / instantánea (sin preparar)',        25, 'g', 7),
((SELECT id FROM g), 'Sopa lista para consumir',                         250, 'ml', 8),
((SELECT id FROM g), 'Levadura (en polvo, fresca)',                       5, 'g',  9),
((SELECT id FROM g), 'Vinagre',                                          15, 'ml', 10);


-- ══════════════════════════════════════════════════════════════════════════
--  3. Alteraciones en la tabla products
-- ══════════════════════════════════════════════════════════════════════════

-- Denominación legal/comercial del alimento (ej. "Galletitas de avena")
ALTER TABLE products
    ADD COLUMN denomination VARCHAR(300);

-- Vínculo con el grupo y alimento de referencia (reemplaza category)
ALTER TABLE products
    ADD COLUMN food_group_id UUID REFERENCES food_groups(id),
    ADD COLUMN food_item_id  UUID REFERENCES food_items(id);

-- RNPA ahora es obligatorio para todos los productos nuevos
-- (no hacemos NOT NULL retroactivo para no romper registros existentes;
--  la validación la maneja el backend en request/service)

-- Toggle: mostrar o no el % de cada ingrediente en el rótulo
ALTER TABLE products
    ADD COLUMN show_ingredient_percentages BOOLEAN NOT NULL DEFAULT false;

-- Tabla nutricional del producto (valores por 100 g; el back calcula por porción)
ALTER TABLE products
    ADD COLUMN energy_kcal_per100g  NUMERIC(8,2),
    ADD COLUMN proteins_g_per100g   NUMERIC(8,2),
    ADD COLUMN carbs_g_per100g      NUMERIC(8,2),
    ADD COLUMN sugars_g_per100g     NUMERIC(8,2),
    ADD COLUMN fat_total_g_per100g  NUMERIC(8,2),
    ADD COLUMN fat_sat_g_per100g    NUMERIC(8,2),
    ADD COLUMN fat_trans_g_per100g  NUMERIC(8,2),
    ADD COLUMN sodium_mg_per100g    NUMERIC(8,2);

-- Índices de performance
CREATE INDEX idx_products_food_group ON products(food_group_id);
CREATE INDEX idx_products_food_item  ON products(food_item_id);


-- ══════════════════════════════════════════════════════════════════════════
--  4. Simplificación de ingredientes: eliminamos los campos nutricionales
--     (la info nutricional ahora vive en el producto, no en cada ingrediente)
-- ══════════════════════════════════════════════════════════════════════════

ALTER TABLE ingredients
    DROP COLUMN IF EXISTS energy_kcal_per100g,
    DROP COLUMN IF EXISTS proteins_g_per100g,
    DROP COLUMN IF EXISTS carbs_g_per100g,
    DROP COLUMN IF EXISTS sugars_g_per100g,
    DROP COLUMN IF EXISTS fat_total_g_per100g,
    DROP COLUMN IF EXISTS fat_sat_g_per100g,
    DROP COLUMN IF EXISTS fat_trans_g_per100g,
    DROP COLUMN IF EXISTS sodium_mg_per100g;
