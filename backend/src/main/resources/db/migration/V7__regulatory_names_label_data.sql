-- ══════════════════════════════════════════════════════════════════════════
--  V7: Tabla de denominaciones legales por categoría CAA + label_data
--
--  Propósito:
--    1. regulatory_name  — lookup de denominación legal del CAA por categoría
--       de producto, con artículo fuente.
--    2. label_projects.label_data — columna TEXT para almacenar el JSON
--       completo del rótulo generado (snapshot inmutable por versión).
--
--  Referencias:
--    - CAA Ley 18.284 y resoluciones SENASA/ANMAT vigentes
--    - CAA Art. 1385 — Claims nutricionales (bajo en, sin, enriquecido con)
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Tabla de denominaciones legales ────────────────────────────────────
CREATE TABLE regulatory_name (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    category       VARCHAR(100) NOT NULL,
    base_name      TEXT         NOT NULL,
    description    TEXT,
    source_article VARCHAR(300) NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_regulatory_name_category ON regulatory_name (category);

-- ── 2. Columna label_data en label_projects ────────────────────────────────
ALTER TABLE label_projects ADD COLUMN label_data TEXT;

-- ══════════════════════════════════════════════════════════════════════════
--  SEED — 35 categorías del CAA validadas
-- ══════════════════════════════════════════════════════════════════════════
INSERT INTO regulatory_name (category, base_name, description, source_article) VALUES

-- ── AGUAS ────────────────────────────────────────────────────────────────
('agua mineral',      'Agua mineral natural',
 'Agua proveniente de fuente subterránea protegida, con composición mineral constante.',
 'CAA Art. 982 bis / Res. ANMAT 2266/2007'),

('agua saborizada',   'Agua saborizada',
 'Bebida elaborada con agua potable con saborizantes. Sin aporte de jugo de fruta.',
 'CAA Art. 1039 / Res. SENASA 6/2010'),

('agua tónica',       'Agua tónica',
 'Bebida carbonatada con quinina. Contenido mínimo de quinina declarado en el rótulo.',
 'CAA Art. 1041'),

-- ── BEBIDAS ───────────────────────────────────────────────────────────────
('jugo de fruta',     'Jugo de [fruta]',
 'Producto líquido no fermentado obtenido de la parte comestible de frutas maduras y sanas. 100% jugo.',
 'CAA Art. 1039 § 1'),

('néctar de fruta',   'Néctar de [fruta]',
 'Producto elaborado con jugo, pulpa o concentrado de fruta. Mínimo 25-50% de jugo según la fruta.',
 'CAA Art. 1040'),

('bebida de fruta',   'Bebida a base de [fruta]',
 'Bebida con contenido de jugo entre 10% y 25%. Declarar porcentaje de jugo en el rótulo.',
 'CAA Art. 1039 § 3'),

('bebida gaseosa',    'Bebida gaseosa',
 'Bebida carbonatada con edulcorantes y saborizantes. Declarar tipo de edulcorante.',
 'CAA Art. 1041 / Ley 27.642'),

('bebida energizante','Bebida energizante',
 'Bebida con cafeína ≥150 mg/L y otros estimulantes. Leyenda "CONTIENE CAFEÍNA" obligatoria.',
 'CAA Art. 1041 bis / Res. CONAL 7/2010'),

('infusión',          'Infusión de [hierba/fruto]',
 'Producto elaborado a partir de partes de plantas destinadas a la preparación de bebidas por infusión.',
 'CAA Art. 1195'),

-- ── LÁCTEOS ───────────────────────────────────────────────────────────────
('leche fluida',      'Leche entera pasteurizada',
 'Leche de vaca pasteurizada con ≥3,0% de materia grasa. Denominación varía según contenido graso.',
 'CAA Art. 559 § 1'),

('leche descremada',  'Leche parcialmente descremada pasteurizada',
 'Leche con contenido graso entre 0,5% y 3,0%. Para <0,5%: "Leche descremada".',
 'CAA Art. 559 § 2'),

('leche en polvo',    'Leche entera en polvo',
 'Producto obtenido por deshidratación de leche entera. Humedad máxima 4%. Proteínas mínimo 34% (b.s.).',
 'CAA Art. 562 § 2'),

('yogur',             'Yogur',
 'Producto obtenido por fermentación de leche con L. delbrueckii ssp. bulgaricus y S. thermophilus.',
 'CAA Art. 576 bis'),

('queso',             'Queso [variedad]',
 'Producto fresco o madurado obtenido por coagulación de leche. Denominación según variedad CAA.',
 'CAA Art. 605 § 2'),

('manteca',           'Manteca',
 'Producto graso elaborado exclusivamente con crema de leche. Mínimo 80% de materia grasa butírica.',
 'CAA Art. 592'),

('crema de leche',    'Crema de leche',
 'Producto lácteo rico en grasa separado de la leche por reposo o centrifugación. Mínimo 18% m.g.',
 'CAA Art. 589'),

('dulce de leche',    'Dulce de leche',
 'Producto elaborado por concentración de leche y azúcar. Mínimo 7% proteínas, 6% grasas.',
 'CAA Art. 786'),

-- ── PANIFICADOS Y CEREALES ────────────────────────────────────────────────
('pan',               'Pan',
 'Producto obtenido por cocción de una masa fermentada de harina de trigo y agua. Sin conservantes si es "fresco".',
 'CAA Art. 726'),

('pan integral',      'Pan integral de trigo',
 'Pan elaborado con ≥51% de harina integral de trigo en la mezcla de harinas.',
 'CAA Art. 727 § 2'),

('galletita',         'Galletita',
 'Producto de panadería obtenido por cocción de masa elaborada con harina, grasas y otros ingredientes.',
 'CAA Art. 752'),

('harina de trigo',   'Harina de trigo [tipo]',
 'Producto obtenido por molienda de trigo. Tipos: 000 (especial), 0000 (extra fina), integral.',
 'CAA Art. 661'),

('pasta seca',        'Fideos [forma] de sémola de trigo',
 'Producto elaborado con sémola o semolín de trigo duro sin fermentación y secado. Humedad ≤13%.',
 'CAA Art. 706'),

('arroz',             'Arroz [variedad]',
 'Grano de Oryza sativa. Denominar según proceso: arroz blanco, integral, parbolizado, etc.',
 'CAA Art. 648'),

('avena',             'Avena arrollada / harina de avena',
 'Producto de Avena sativa. Declarar si contiene gluten en planta con TACC.',
 'CAA Art. 660 / Res. ANMAT 4238/2011'),

('maíz procesado',    'Copos de maíz',
 'Producto obtenido por expansión de granos de maíz. Declarar sal y azúcares añadidos.',
 'CAA Art. 650'),

-- ── CARNES Y DERIVADOS ────────────────────────────────────────────────────
('carne vacuna',      'Carne vacuna [corte]',
 'Carne de bovino adulto. Denominar por corte según Res. SENASA 871/2018.',
 'CAA Art. 255 / Res. SENASA 871/2018'),

('pollo',             'Pollo entero / trozado',
 'Carne de ave de la especie Gallus domesticus. Declarar si es fresco, refrigerado o congelado.',
 'CAA Art. 302 § 3'),

('fiambre cocido',    'Jamón cocido [categoría]',
 'Producto cárnico cocido. Categorías: calidad máxima, primera calidad, segunda calidad según contenido proteico.',
 'CAA Art. 325'),

('embutido',          'Salchicha tipo [variedad]',
 'Embutido fresco o cocido. Declarar especie animal, contenido de sal y aditivos.',
 'CAA Art. 302 / CAA Art. 359'),

-- ── CONSERVAS ─────────────────────────────────────────────────────────────
('conserva de pescado','Conserva de [especie] en [medio de cobertura]',
 'Producto esterilizado en envase hermético. Declarar especie, presentación y líquido de cobertura.',
 'CAA Art. 960'),

('conserva de tomate','Tomate triturado / pulpa de tomate',
 'Producto elaborado con tomates frescos. Declarar concentración de sólidos solubles (°Brix).',
 'CAA Art. 948 / CAA Art. 950'),

-- ── CONDIMENTOS Y GRASAS ──────────────────────────────────────────────────
('aceite vegetal',    'Aceite de [especie vegetal]',
 'Aceite comestible de origen vegetal. Declarar especie botánica. Prohibida denominación genérica "aceite vegetal".',
 'CAA Art. 519 / CAA Art. 155 § 4'),

('vinagre',           'Vinagre de [origen]',
 'Condimento líquido ácido obtenido por fermentación acética. Acidez mínima 40 g/L de ácido acético.',
 'CAA Art. 1198'),

('sal de mesa',       'Sal fina yodada',
 'Cloruro de sodio con yodato de potasio añadido obligatoriamente. Yodo: 25-40 mg/kg.',
 'CAA Art. 1238 / Ley 17.259'),

-- ── DULCES Y CONFITERÍA ───────────────────────────────────────────────────
('mermelada',         'Mermelada de [fruta]',
 'Producto elaborado por cocción de frutas enteras o en trozos con azúcar. Mínimo 35% de fruta.',
 'CAA Art. 807'),

('chocolate',         'Chocolate [tipo]',
 'Producto elaborado con pasta de cacao, manteca de cacao y azúcar. Mínimo 35% de sólidos de cacao.',
 'CAA Art. 779'),

('maní tostado',      'Maní tostado [con/sin sal]',
 'Maní (Arachis hypogaea) sometido a proceso de tostado. Declarar alérgeno MANÍ obligatoriamente.',
 'CAA Art. 909 / Res. 109/2023');
