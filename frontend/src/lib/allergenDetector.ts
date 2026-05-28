/**
 * Detección client-side de alérgenos según Res. 109/2023 (Argentina).
 * Misma lógica que AllergenDetector.java en el backend.
 */
const KEYWORDS: string[] = [
  // Cereales con gluten
  'trigo', 'centeno', 'cebada', 'avena', 'espelta', 'kamut', 'farro',
  'triticale', 'gluten', 'harina de trigo', 'almidón de trigo',
  'sémola', 'semola', 'cuscús', 'cuscus', 'bulgur',
  // derivados frecuentes no cubiertos por el nombre del cereal
  'malta', 'extracto de malta', 'cebada malteada',
  'salvado de trigo', 'salvado de avena', 'germen de trigo',
  'proteína de trigo', 'gluten de trigo', 'copos de avena',

  // Crustáceos
  'crustáceo', 'crustaceo', 'camarón', 'camaron', 'langosta',
  'langostino', 'cangrejo', 'krill', 'cigala', 'bogavante',
  'gamba', 'quisquilla',

  // Huevo
  'huevo', 'albumina', 'ovoalbúmina', 'ovoalbumina', 'lisozima',
  'ovomucina', 'ovomucoide',
  'mayonesa', 'yema', 'clara de huevo', 'lecitina de huevo',

  // Pescado
  'pescado', 'anchoa', 'anchoíta', 'anchoita', 'bacalao', 'merluza',
  'atún', 'atun', 'salmón', 'salmon', 'sardina', 'tilapia',
  'surimi', 'trucha', 'lenguado', 'pejerrey', 'dorado',
  // especies frecuentes en Argentina
  'corvina', 'boga', 'pacú', 'pacu', 'surubí', 'surubi', 'bagre',

  // Maní
  'maní', 'mani', 'cacahuate', 'cacahuete', 'mantequilla de maní',

  // Soja
  'soja', 'soya', 'lecitina de soja', 'proteína de soja',
  'harina de soja', 'tofu',
  'edamame', 'miso', 'tempeh',

  // Leche
  'leche', 'lactosa', 'suero de leche', 'caseína', 'caseina',
  'caseínato', 'caseinato',
  'lactosuero', 'suero lácteo',
  'lactoalbúmina', 'lactoalbumina', 'lactoglobulina',
  'manteca', 'mantequilla', 'queso', 'crema', 'yogur', 'yogurt',
  'ricotta', 'requesón', 'requeson', 'nata',

  // Frutos de cáscara (según Res. 109/2023: almendra, avellana, nuez, anacardo,
  // pecán, nuez de Brasil, pistacho, macadamia/Queensland)
  'almendra', 'avellana', 'anacardo', 'cajú', 'caju',
  'pistacho', 'nuez de brasil', 'nuez de macadamia',
  'nuez de pecán', 'nuez de pecan', 'pecán', 'pecan',
  'nuez',

  // Apio
  'apio',

  // Mostaza
  'mostaza',

  // Sésamo
  'sésamo', 'sesamo', 'ajonjolí', 'ajonjoli', 'tahini', 'tahín', 'tahin',

  // Sulfitos
  'dióxido de azufre', 'dioxido de azufre',
  'sulfito', 'bisulfito', 'metabisulfito',

  // Altramuces
  'altramuz', 'lupino', 'lupini', 'altramuces',

  // Moluscos
  'molusco', 'almeja', 'mejillón', 'mejillon', 'ostra',
  'calamar', 'pulpo', 'caracol', 'berberecho', 'vieira',
  'chipirón', 'chipiron', 'jibia', 'sepia',
]

/** Términos que contienen una keyword pero NO son alérgenos según la Res. 109/2023. */
const EXCLUSIONS: string[] = [
  'nuez moscada',  // especia, no fruto de cáscara
]

export function detectAllergen(name: string): boolean {
  if (!name.trim()) return false
  const lower = name.toLowerCase()
  if (EXCLUSIONS.some((ex) => lower.includes(ex))) return false
  return KEYWORDS.some((kw) => lower.includes(kw))
}
