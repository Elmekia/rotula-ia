/**
 * Detección client-side de alérgenos según Res. 109/2023 (Argentina).
 * Misma lógica que AllergenDetector.java en el backend.
 */
const KEYWORDS: string[] = [
  // Cereales con gluten
  'trigo', 'centeno', 'cebada', 'avena', 'espelta', 'kamut', 'farro',
  'triticale', 'gluten', 'harina de trigo', 'almidón de trigo',
  'sémola', 'cuscús', 'bulgur',

  // Crustáceos
  'crustáceo', 'crustaceo', 'camarón', 'camaron', 'langosta',
  'langostino', 'cangrejo', 'krill', 'cigala', 'bogavante',

  // Huevo
  'huevo', 'albumina', 'ovoalbúmina', 'ovoalbumina', 'lisozima',
  'mayonesa', 'yema', 'clara de huevo',

  // Pescado
  'pescado', 'anchoa', 'anchoíta', 'anchoita', 'bacalao', 'merluza',
  'atún', 'atun', 'salmón', 'salmon', 'sardina', 'tilapia',
  'surimi', 'trucha', 'lenguado', 'pejerrey', 'dorado',

  // Maní
  'maní', 'mani', 'cacahuate', 'cacahuete',

  // Soja
  'soja', 'soya', 'lecitina de soja', 'proteína de soja',
  'harina de soja', 'tofu',

  // Leche
  'leche', 'lactosa', 'suero de leche', 'caseína', 'caseina',
  'lactoalbúmina', 'lactoalbumina', 'lactoglobulina', 'manteca',
  'mantequilla', 'queso', 'crema', 'yogur', 'yogurt', 'ricotta',
  'requesón', 'requeson', 'nata',

  // Frutos de cáscara
  'nuez', 'almendra', 'avellana', 'anacardo', 'cajú', 'caju',
  'pistacho', 'nuez de brasil', 'nuez de macadamia',
  'nuez de pecán', 'nuez de pecan', 'pecán', 'pecan',

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
]

export function detectAllergen(name: string): boolean {
  if (!name.trim()) return false
  const lower = name.toLowerCase()
  return KEYWORDS.some((kw) => lower.includes(kw))
}
