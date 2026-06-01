// ── Datos de referencia TABLA I ───────────────────────────────────────────────

export interface FoodGroup {
  id:        string
  code:      string
  name:      string
  sortOrder: number
}

export interface FoodItem {
  id:           string
  foodGroupId:  string
  name:         string
  /** Porción de referencia en gramos o mL (TABLA I – Res. Conjunta 21/2023). */
  portionGrams: number
  /** Unidad de la porción: 'g' o 'ml'. */
  unit:         string
  sortOrder:    number
}

// ── Producto ──────────────────────────────────────────────────────────────────

export interface Product {
  id:        string
  tenantId:  string

  /** Nombre interno (no aparece en el rótulo). */
  name:        string
  /** Denominación del alimento según CAA (aparece en el rótulo). */
  denomination: string

  // Clasificación TABLA I
  foodGroupId: string
  foodItemId:  string
  /** Porción de referencia (auto-completada desde el food_item seleccionado). */
  servingSizeG: number

  // Presentación
  netWeight:   number
  weightUnit:  string

  // Registros
  rneNumber:  string | null
  rnpaNumber: string       // obligatorio

  // Contaminación cruzada (lista de nombres del enum AllergenGroup)
  crossContaminationGroups: string[]

  // Opciones de rótulo
  showIngredientPercentages: boolean

  // Tabla nutricional por 100 g (todos opcionales)
  energyKcalPer100g: number | null
  proteinsPer100g:   number | null
  carbsPer100g:      number | null
  sugarsPer100g:     number | null
  fatTotalPer100g:   number | null
  fatSatPer100g:     number | null
  fatTransPer100g:   number | null
  sodiumMgPer100g:   number | null

  status:    string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name:         string
  denomination: string

  foodGroupId: string
  foodItemId:  string
  servingSizeG?: number | null

  netWeight:  number
  weightUnit: string

  rneNumber?:  string | null
  rnpaNumber:  string

  crossContaminationGroups: string[]
  showIngredientPercentages: boolean

  energyKcalPer100g?: number | null
  proteinsPer100g?:   number | null
  carbsPer100g?:      number | null
  sugarsPer100g?:     number | null
  fatTotalPer100g?:   number | null
  fatSatPer100g?:     number | null
  fatTransPer100g?:   number | null
  sodiumMgPer100g?:   number | null
}

export interface PageResponse<T> {
  content:       T[]
  page:          number
  size:          number
  totalElements: number
  totalPages:    number
  last:          boolean
}

// ── Label analysis types ──────────────────────────────────────────────────────

export interface RoundedNutritionValues {
  energyKcal: number
  energyKj:   number
  proteinsG:  number
  carbsG:     number
  sugarsG:    number
  fatTotalG:  number
  fatSatG:    number
  fatTransG:  number
  sodiumMg:   number
}

export interface NutritionValues extends RoundedNutritionValues {}

export interface NutritionCalculationResult {
  servingSizeG: number
  perPortion:   RoundedNutritionValues
  per100g:      RoundedNutritionValues
  rawPer100g:   NutritionValues
}

export interface AllergenDeclarationResult {
  presentGroups:            string[]
  crossContaminationGroups: string[]
  declarationText:          string
}

export interface LabelAnalysisResult {
  productId: string
  nutrition: NutritionCalculationResult | null
  seals:     string[]
  allergens: AllergenDeclarationResult
}
