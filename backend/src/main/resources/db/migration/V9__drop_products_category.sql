-- V9: Elimina la columna category de products.
--
-- La categoría fue reemplazada en V8 por food_group_id + food_item_id
-- (clasificación según TABLA I – Res. Conjunta 21/2023).
-- La columna tenía NOT NULL en V1 y no fue removida en V8,
-- lo que generaba un constraint violation al insertar nuevos productos.

ALTER TABLE products DROP COLUMN IF EXISTS category;
