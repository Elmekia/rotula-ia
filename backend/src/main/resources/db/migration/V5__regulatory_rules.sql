-- ══════════════════════════════════════════════════════════════════════════
--  V5: Tabla de reglas normativas del CAA / Res. CONAL
--
--  Propósito: modelar las obligaciones legales de etiquetado como registros
--  versionados, aplicables sin modificar código (evaluación por DSL).
--
--  Referencias principales:
--    - Código Alimentario Argentino (CAA) — Ley 18.284 y sus actualizaciones
--    - Ley 27.642 / Decreto 151/2022 — Etiquetado frontal de alimentos
--    - Res. CONAL 683/2021 — Sellos de advertencia
--    - Res. Conjunta 1/2023 (Res. 109/2023) — Alérgenos de declaración obligatoria
-- ══════════════════════════════════════════════════════════════════════════

-- ── Tabla ─────────────────────────────────────────────────────────────────
CREATE TABLE regulatory_rule (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Código único de la regla. Formato: {CATEGORIA}-{NNN}
    -- ej.: BEB-001, LAC-003, SNK-002
    code            VARCHAR(20) NOT NULL,

    -- Categoría de producto a la que aplica; NULL = aplica a todas
    category        VARCHAR(50),

    -- Descripción legible de la obligación
    description     TEXT        NOT NULL,

    -- Condición de aplicabilidad en DSL JSON estructurado.
    -- Evaluada en tiempo de generación de rótulo para determinar si
    -- la regla aplica al producto concreto.
    --
    -- Estructura:
    --   { "category": ["bebidas"] }                  → por categoría
    --   { "ingredientNameContains": "cafeína" }       → por ingrediente
    --   { "nutrient": "sodium_mg_per_100g",
    --     "operator": "gte", "threshold": 400 }       → umbral nutricional
    --   { "productClaim": "sin gluten" }              → por claim del producto
    condition_dsl   TEXT        NOT NULL,

    -- Tipo de acción que impone la regla:
    --   DECLARAR_LEYENDA_OBLIGATORIA   → texto obligatorio en rótulo
    --   INCLUIR_SELLO_ADVERTENCIA      → sello octogonal Ley 27.642
    --   VERIFICAR_DENOMINACION_LEGAL   → denominación legal requerida
    --   DECLARAR_COMPOSICION           → declarar composición específica
    --   ADVERTENCIA_ALERGENO           → leyenda de alérgeno
    --   DECLARAR_ADITIVO               → declarar aditivo específico
    --   DECLARAR_NUTRIENTE             → declarar nutriente en tabla
    --   VERIFICAR_COMPOSICION          → restricción de composición
    action          VARCHAR(50) NOT NULL
        CONSTRAINT regulatory_rule_action_check CHECK (action IN (
            'DECLARAR_LEYENDA_OBLIGATORIA',
            'INCLUIR_SELLO_ADVERTENCIA',
            'VERIFICAR_DENOMINACION_LEGAL',
            'DECLARAR_COMPOSICION',
            'ADVERTENCIA_ALERGENO',
            'DECLARAR_ADITIVO',
            'DECLARAR_NUTRIENTE',
            'VERIFICAR_COMPOSICION'
        )),

    -- Detalle textual de la acción (leyenda exacta, nombre del sello, etc.)
    action_detail   TEXT,

    -- Artículo fuente en el cuerpo normativo
    source_article  VARCHAR(300) NOT NULL,

    -- Vigencia de la versión de esta regla
    valid_from      DATE        NOT NULL DEFAULT CURRENT_DATE,
    valid_until     DATE,            -- NULL = vigente indefinidamente

    -- Número de versión para trazabilidad de cambios normativos
    version         INTEGER     NOT NULL DEFAULT 1,

    -- Regla activa o archivada
    active          BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Notas del especialista en alimentos / validación normativa
    notes           TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT regulatory_rule_code_version_uq UNIQUE (code, version)
);

CREATE INDEX idx_regulatory_rule_category ON regulatory_rule (category);
CREATE INDEX idx_regulatory_rule_active    ON regulatory_rule (active);


-- ══════════════════════════════════════════════════════════════════════════
--  SEED — Reglas iniciales por categoría (validadas según CAA vigente)
--  Fuente de validación: Resoluciones CONAL, CAA y Ley 27.642
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. BEBIDAS ─────────────────────────────────────────────────────────────
INSERT INTO regulatory_rule
    (code, category, description, condition_dsl, action, action_detail, source_article, valid_from, notes)
VALUES

('BEB-001', 'bebidas',
 'Bebidas con cafeína añadida: leyenda "CONTIENE CAFEÍNA" obligatoria',
 '{"category":["bebidas"],"ingredientNameContains":"cafeína"}',
 'DECLARAR_LEYENDA_OBLIGATORIA',
 'CONTIENE CAFEÍNA',
 'Ley 27.642 Art. 5 inc. d / Res. CONAL 7/2010 Art. 3',
 '2022-06-20',
 'Aplica a bebidas energéticas y toda bebida con cafeína añadida (≥150 mg/L). '
 'La leyenda debe figurar en el panel frontal con tipografía no menor a 1,5 mm. '
 'Validado por especialista: Jul-2024.'),

('BEB-002', 'bebidas',
 'Bebidas con edulcorantes no calóricos: leyenda obligatoria',
 '{"category":["bebidas"],"ingredientIsNonCaloricSweetener":true}',
 'DECLARAR_LEYENDA_OBLIGATORIA',
 'CONTIENE EDULCORANTE/S. NO RECOMENDADO PARA NIÑOS',
 'Ley 27.642 Art. 5 inc. e / CAA Art. 1385 / Res. CONAL 683/2021 Anexo IV',
 '2023-08-01',
 'Aplica cuando el producto contiene edulcorantes no calóricos (aspartamo, acesulfamo K, '
 'sucralosa, estevia, sacarina, ciclamato). La leyenda "No recomendado para niños" es '
 'obligatoria desde Fase 1 (ago-2023). Validado por especialista: Jul-2024.'),

('BEB-003', 'bebidas',
 'Jugos y néctares: declaración del porcentaje de jugo de fruta',
 '{"category":["bebidas"],"productType":["jugo","néctar","nectar","bebida de fruta"]}',
 'DECLARAR_COMPOSICION',
 'Declarar "CONTIENE X% DE JUGO DE [FRUTA]" en proximidad a la denominación del producto.',
 'CAA Art. 1039 § 3 / CAA Art. 1040 § 2',
 '2018-01-01',
 'Umbrales mínimos: jugo puro = 100%; néctar = 25-99% (varía por fruta); '
 'bebida de fruta = 10-24%. El porcentaje debe figurar en el panel principal. '
 'Validado por especialista: Jul-2024.'),

('BEB-004', 'bebidas',
 'Aguas minerales naturales: declaración de composición iónica obligatoria',
 '{"category":["bebidas","aguas"],"productType":["agua mineral","agua mineral natural"]}',
 'DECLARAR_COMPOSICION',
 'Declarar mineralización total y composición iónica (Na⁺, Mg²⁺, Ca²⁺, K⁺, Cl⁻, SO₄²⁻, HCO₃⁻, NO₃⁻) en mg/L.',
 'CAA Art. 982 bis / Res. ANMAT 2266/2007',
 '2007-01-01',
 'La composición química debe corresponder al análisis del agua en el punto de captación. '
 'Incluir número de expediente ANMAT de autorización del producto. '
 'Validado por especialista: Jul-2024.'),

('BEB-005', 'bebidas',
 'Bebidas con colorantes artificiales: declaración específica por nombre',
 '{"category":["bebidas"],"ingredientIsArtificialColorant":true}',
 'DECLARAR_ADITIVO',
 'Declarar nombre IUPAC o INS del colorante artificial (ej.: "tartrazina (INS 102)", "rojo allura (INS 129)").',
 'CAA Art. 1388 § 2 / Res. SENASA 1291/2013 Art. 4',
 '2013-06-01',
 'Prohibida la declaración genérica "colorante artificial" sin identificar la sustancia. '
 'Los colorantes azo (tartrazina, amarillo ocaso, rojo allura, etc.) requieren además la '
 'leyenda "puede afectar la actividad y la atención en niños" según la UE — en Argentina '
 'es recomendación CONAL vigente. Validado por especialista: Jul-2024.'),


-- ── 2. LÁCTEOS ─────────────────────────────────────────────────────────────

('LAC-001', 'lacteos',
 'Leche fluida: denominación legal según contenido graso',
 '{"category":["lacteos"],"productType":["leche fluida","leche pasteurizada","leche esterilizada","leche UHT"]}',
 'VERIFICAR_DENOMINACION_LEGAL',
 '"Leche entera" (≥3,0% m.g.) | "Leche parcialmente descremada" (1,5–3,0%) | "Leche descremada" (<0,5%).',
 'CAA Art. 559 § 1 y § 2',
 '2000-01-01',
 'La denominación de venta debe corresponder exactamente al rango de grasa post-proceso. '
 'Prohibido usar denominaciones no contempladas en el CAA Art. 559. '
 'Validado por especialista: Jul-2024.'),

('LAC-002', 'lacteos',
 'Yogur: declaración de cultivos de bacterias lácticas específicas',
 '{"category":["lacteos"],"productType":["yogur","yogurt"]}',
 'DECLARAR_COMPOSICION',
 'Declarar presencia activa de Lactobacillus delbrueckii subsp. bulgaricus y Streptococcus thermophilus (≥10⁷ UFC/g).',
 'CAA Art. 576 bis § 3',
 '2006-06-01',
 'Solo puede denominarse "yogur" el producto obtenido con los dos cultivos específicos y con '
 'recuento mínimo de 10^7 UFC/g en el momento del consumo (fecha de vencimiento). '
 'Los yogures con cultivos adicionales (bifidobacterias, etc.) los deben declarar por separado. '
 'Validado por especialista: Jul-2024.'),

('LAC-003', 'lacteos',
 'Quesos fundidos y procesados: declarar queso(s) base y porcentaje',
 '{"category":["lacteos"],"productType":["queso fundido","queso procesado","queso para untar","crema de queso"]}',
 'DECLARAR_COMPOSICION',
 'Indicar variedad(es) de queso de origen y porcentaje de participación en orden decreciente.',
 'CAA Art. 605 § 4 / Res. SENASA 780/2018',
 '2018-01-01',
 'Los quesos fundidos son mezclas de quesos naturales sometidos a fusión. La materia prima '
 'no puede ser reemplazada por análogos sin que esto figure en la denominación. '
 'Validado por especialista: Jul-2024.'),

('LAC-004', 'lacteos',
 'Leche en polvo: declarar proteínas y humedad en rótulo nutricional',
 '{"category":["lacteos"],"productType":["leche en polvo","leche entera en polvo","leche descremada en polvo"]}',
 'DECLARAR_NUTRIENTE',
 'Declarar proteínas totales (mínimo 34% en base seca) y humedad máxima (4% entera / 5% descremada).',
 'CAA Art. 562 § 2 y § 3',
 '2000-01-01',
 'Los parámetros de proteínas y humedad son indicadores de calidad e inocuidad y deben '
 'estar presentes en la tabla nutricional. Proteínas < 34% en base seca indica adulteración. '
 'Validado por especialista: Jul-2024.'),

('LAC-005', 'lacteos',
 'Quesos con denominación de variedad: cumplimiento de identidad y calidad',
 '{"category":["lacteos"],"productType":["queso"],"productNameContains":["roquefort","brie","camembert","parmesano","gruyere","emmental","gouda","edam"]}',
 'VERIFICAR_DENOMINACION_LEGAL',
 'El producto debe cumplir los requisitos tecnológicos (maduración, humedad, grasa, microbiota) definidos para cada variedad en el CAA.',
 'CAA Art. 605 § 2 / Normas MERCOSUR de identidad y calidad de quesos',
 '2001-01-01',
 'Ejemplo: Roquefort requiere moho Penicillium roqueforti y maduración específica. '
 'Parmesano: maduración mínima 10 meses, humedad ≤32%, grasa ≥32%. '
 'El uso de denominaciones fuera de norma es infracción al CAA. '
 'Validado por especialista: Jul-2024.'),


-- ── 3. PANIFICADOS ─────────────────────────────────────────────────────────

('PAN-001', 'panificados',
 'Pan fresco: prohibición de conservantes antimicrobianos',
 '{"category":["panificados"],"productType":["pan fresco","pan artesanal","pan de panadería"]}',
 'VERIFICAR_COMPOSICION',
 'Verificar ausencia de conservantes (propionatos, sorbatos, benzoatos) en la lista de ingredientes.',
 'CAA Art. 726 § 4',
 '1998-01-01',
 'Solo puede denominarse "pan fresco" el elaborado y expendido el mismo día, sin conservantes. '
 'El pan con propionato de calcio u otros conservantes debe denominarse "pan de molde", '
 '"pan lactal" u otra denominación que no incluya "fresco". '
 'Validado por especialista: Jul-2024.'),

('PAN-002', 'panificados',
 'Pan denominado por cereal: mínimo 51% de la harina del cereal declarado',
 '{"category":["panificados"],"productNameContains":["integral","centeno","maíz","salvado","avena","multicereal"]}',
 'VERIFICAR_COMPOSICION',
 'El cereal que figura en la denominación debe representar ≥51% de la mezcla de harinas utilizada.',
 'CAA Art. 727 § 2',
 '1998-01-01',
 'Pan de centeno: ≥51% harina de centeno. Pan integral: ≥51% harina integral de trigo. '
 'Pan de salvado: la harina puede ser refinada con salvado añadido, pero el salvado debe '
 'ser el ingrediente diferenciador declarado en la denominación. '
 'Validado por especialista: Jul-2024.'),

('PAN-003', 'panificados',
 'Harina de trigo enriquecida: declaración de micronutrientes añadidos',
 '{"category":["panificados","harinas"],"ingredientNameContains":"harina enriquecida"}',
 'DECLARAR_NUTRIENTE',
 'Declarar en tabla nutricional: hierro (mg), ácido fólico (μg), tiamina (mg), riboflavina (mg), niacina (mg) por 100g.',
 'Ley 25.630 Art. 4 / Res. SENASA 105/2010 Anexo',
 '2002-01-01',
 'La Ley 25.630 hace obligatorio el enriquecimiento de harina de trigo con hierro, '
 'ácido fólico y vitaminas del complejo B. Los niveles mínimos están en Res. 105/2010. '
 'Todo panificado elaborado con harina enriquecida debe reflejarlo en la tabla nutricional. '
 'Validado por especialista: Jul-2024.'),

('PAN-004', 'panificados',
 'Panificados "Sin TACC" / sin gluten: leyenda y símbolo obligatorio',
 '{"category":["panificados"],"productClaim":"sin gluten"}',
 'DECLARAR_LEYENDA_OBLIGATORIA',
 'Incluir leyenda "SIN TACC" o "LIBRE DE GLUTEN" con símbolo oficial de espiga barrada (ARCELA). Gluten residual ≤10 mg/kg.',
 'CAA Art. 1383 / Res. ANMAT 4238/2011 Art. 5',
 '2011-01-01',
 'El símbolo de espiga barrada es obligatorio para productos certificados. '
 'Si el establecimiento también procesa productos con TACC, debe declararse '
 '"Elaborado en planta que también procesa trigo, avena, cebada y centeno". '
 'Gluten residual medido por método ELISA R5 Mendez (AOAC 2012.01). '
 'Validado por especialista: Jul-2024.'),

('PAN-005', 'panificados',
 'Galletitas y panificados: declaración de grasas trans en tabla nutricional',
 '{"category":["panificados"],"productType":["galletita","cookie","oblea","facturas","bizcochos"]}',
 'DECLARAR_NUTRIENTE',
 'Declarar grasas trans (ácidos grasos trans) en la tabla nutricional, bajo el ítem grasas saturadas.',
 'CAA Art. 1388 § 5 / Res. CONAL 241/2019',
 '2019-01-01',
 'Res. 241/2019 prohíbe el uso de aceites parcialmente hidrogenados (APH) industriales. '
 'Si el producto contiene APH residuales, las grasas trans deben declararse con valor ≥0. '
 'Valor 0 solo admisible si el contenido es <0,2g/porción (se declara como "0 g"). '
 'Validado por especialista: Jul-2024.'),


-- ── 4. CONSERVAS ───────────────────────────────────────────────────────────

('CON-001', 'conservas',
 'Conservas en aceite: declarar tipo específico de aceite como ingrediente',
 '{"category":["conservas"],"ingredientNameContains":"aceite"}',
 'DECLARAR_COMPOSICION',
 'Identificar el aceite por su origen vegetal: "aceite de girasol", "aceite de oliva", etc. Prohibida la declaración genérica "aceite vegetal".',
 'CAA Art. 960 § 1 / CAA Art. 155 § 4',
 '1998-01-01',
 'La especificación del tipo de aceite es esencial para alertar a consumidores con alergias '
 '(ej.: alérgicos a maní deben evitar aceite de maní). La declaración genérica "aceite '
 'vegetal" no es admisible en conservas según CAA Art. 960. '
 'Validado por especialista: Jul-2024.'),

('CON-002', 'conservas',
 'Conservas con líquido de cobertura: declarar peso neto y peso escurrido',
 '{"category":["conservas"],"hasLiquidCovering":true}',
 'DECLARAR_COMPOSICION',
 'Declarar en el rótulo principal: "Peso neto: X g" y "Peso escurrido: Y g" (o equivalente en mL para líquidos).',
 'CAA Art. 956 § 2 / Res. MERCOSUR GMC 26/2003 Art. 6',
 '2003-01-01',
 'El peso neto incluye el líquido de cobertura; el peso escurrido es el contenido sólido. '
 'Ambos son obligatorios para conservas en líquido (en aceite, en agua, en salmuera, en '
 'almíbar). La tolerancia admisible es ±10% para peso escurrido < 250g. '
 'Validado por especialista: Jul-2024.'),

('CON-003', 'conservas',
 'Conservas que requieren refrigeración post-apertura: leyenda de conservación',
 '{"category":["conservas"],"requiresRefrigerationAfterOpening":true}',
 'DECLARAR_LEYENDA_OBLIGATORIA',
 '"Una vez abierto, conservar refrigerado entre 0°C y 4°C y consumir dentro de [N] días."',
 'CAA Art. 963 / Res. MERCOSUR GMC 26/2003 Art. 8',
 '2003-01-01',
 'El número de días depende del producto y debe estar validado por el fabricante mediante '
 'estudios de vida útil. La leyenda debe estar en el rótulo secundario o lateral. '
 'Para conservas de pH > 4,5 que no son tratadas térmicamente post-llenado, la refrigeración '
 'post-apertura es crítica para la inocuidad. Validado por especialista: Jul-2024.'),

('CON-004', 'conservas',
 'Conservas con sulfitos > 10 mg/kg: declaración como alérgeno obligatoria',
 '{"category":["conservas"],"ingredientNameContains":["dióxido de azufre","sulfito","bisulfito","metabisulfito"]}',
 'ADVERTENCIA_ALERGENO',
 '"CONTIENE SULFITOS" (si SO₂ equivalente > 10 mg/kg). Usar nombre químico e INS correspondiente.',
 'Res. 109/2023 (Res. Conjunta 1/2023) Grupo 12 / CAA Art. 821',
 '2023-01-01',
 'Los sulfitos y dióxido de azufre son alérgenos de declaración obligatoria según Res. 109/2023. '
 'Incluyen: SO₂ (INS 220), Na₂SO₃ (INS 221), NaHSO₃ (INS 222), Na₂S₂O₅ (INS 223), '
 'K₂S₂O₅ (INS 224), CaSO₃ (INS 226), CaHSO₃ (INS 227), KNO₃ (INS 228). '
 'Validado por especialista: Jul-2024.'),

('CON-005', 'conservas',
 'Conservas cárnicas mixtas: declarar especie animal y % de participación',
 '{"category":["conservas"],"productType":["conserva cárnica","paté","fiambre enlatado","jamón enlatado","mortadela enlatada"]}',
 'DECLARAR_COMPOSICION',
 'Declarar cada especie animal presente (vacuno, porcino, aviar, etc.) y su porcentaje en orden decreciente.',
 'CAA Art. 302 § 3 / CAA Art. 891 § 1',
 '2000-01-01',
 'Para conservas cárnicas con dos o más especies, la denominación debe incluir todas las '
 'especies presentes. El porcentaje de cada especie debe figurar en la lista de ingredientes '
 'o adyacente a la denominación. Prohibido denominar como una sola especie si contiene mezcla. '
 'Validado por especialista: Jul-2024.'),


-- ── 5. SNACKS ──────────────────────────────────────────────────────────────

('SNK-001', 'snacks',
 'Exceso de sodio en alimentos sólidos: sello de advertencia obligatorio',
 '{"category":["snacks"],"nutrient":"sodium_mg_per_100g","operator":"gte","threshold":400}',
 'INCLUIR_SELLO_ADVERTENCIA',
 'EXCESO EN SODIO',
 'Ley 27.642 Art. 5 / Decreto 151/2022 Anexo I § 1.1',
 '2023-08-01',
 'Umbral Fase 1 (ago-2023): ≥400 mg de sodio por 100g de producto sólido. '
 'Para líquidos el umbral es ≥100 mg/100mL. '
 'Sello octogonal negro, borde blanco, tipografía Arial Bold, tamaño mínimo 8% del panel frontal. '
 'Fase 2 (ago-2025): el umbral baja a ≥300 mg/100g. Validado por especialista: Jul-2024.'),

('SNK-002', 'snacks',
 'Exceso de grasas saturadas en alimentos sólidos: sello de advertencia',
 '{"category":["snacks"],"nutrient":"saturated_fat_g_per_100g","operator":"gte","threshold":10}',
 'INCLUIR_SELLO_ADVERTENCIA',
 'EXCESO EN GRASAS SATURADAS',
 'Ley 27.642 Art. 5 / Decreto 151/2022 Anexo I § 1.2',
 '2023-08-01',
 'Umbral Fase 1: ≥10g de grasas saturadas por 100g (sólidos) o ≥3g/100mL (líquidos). '
 'Se suman todos los ácidos grasos saturados incluyendo los de cadena media (C6-C12). '
 'Grasas trans artificiales cuentan como saturadas a los efectos del sello. '
 'Fase 2 (ago-2025): umbral baja a ≥6g/100g. Validado por especialista: Jul-2024.'),

('SNK-003', 'snacks',
 'Exceso de azúcares en alimentos sólidos: sello de advertencia',
 '{"category":["snacks"],"nutrient":"total_sugars_g_per_100g","operator":"gte","threshold":10}',
 'INCLUIR_SELLO_ADVERTENCIA',
 'EXCESO EN AZÚCARES',
 'Ley 27.642 Art. 5 / Decreto 151/2022 Anexo I § 1.3',
 '2023-08-01',
 'Umbral Fase 1: ≥10g de azúcares totales por 100g (sólidos) o ≥5g/100mL (líquidos). '
 'Incluye todos los monosacáridos y disacáridos, tanto naturales como añadidos. '
 'El cálculo se hace sobre el producto tal como se comercializa. '
 'Fase 2 (ago-2025): umbral baja a ≥5g/100g sólidos. Validado por especialista: Jul-2024.'),

('SNK-004', 'snacks',
 'Exceso de calorías en alimentos sólidos: sello de advertencia',
 '{"category":["snacks"],"nutrient":"energy_kcal_per_100g","operator":"gte","threshold":275}',
 'INCLUIR_SELLO_ADVERTENCIA',
 'EXCESO EN CALORÍAS',
 'Ley 27.642 Art. 5 / Decreto 151/2022 Anexo I § 1.4',
 '2023-08-01',
 'Umbral Fase 1: ≥275 kcal por 100g (sólidos) o ≥70 kcal/100mL (líquidos). '
 'Factores de conversión CAA: proteínas 4 kcal/g, grasas 9 kcal/g, carbohidratos 4 kcal/g, '
 'fibra dietaria 2 kcal/g, polialcoholes 2,4 kcal/g. '
 'Fase 2 (ago-2025): umbral baja a ≥250 kcal/100g. Validado por especialista: Jul-2024.'),

('SNK-005', 'snacks',
 'Snacks fritos: declarar tipo de aceite de fritura como ingrediente',
 '{"category":["snacks"],"processingMethod":"frito"}',
 'DECLARAR_ADITIVO',
 'Declarar el aceite de fritura como ingrediente con su nombre botánico (ej.: "aceite de girasol alto oleico").',
 'CAA Art. 155 § 4 / CAA Art. 1388 § 2',
 '2000-01-01',
 'El aceite de fritura industrial debe declararse por su nombre específico; no se admite '
 '"aceite vegetal" genérico. Si se usa una mezcla de aceites, declarar todos en orden '
 'decreciente de proporción. Indicar si el aceite es refinado, prensado en frío, etc. '
 'cuando esto sea relevante para alergias (ej.: aceite de maní, aceite de soja). '
 'Validado por especialista: Jul-2024.');
