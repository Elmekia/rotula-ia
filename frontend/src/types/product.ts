export interface Product {
  id: string
  tenantId: string
  name: string
  category: string
  netWeight: number
  weightUnit: string
  rneNumber: string | null
  rnpaNumber: string | null
  status: string
  /** Tamaño de porción en gramos/mL (opcional, requerido para tabla nutricional). */
  servingSizeG: number | null
  /** Grupos de alérgenos por contaminación cruzada (nombres del enum, separados por coma). */
  crossContamination: string | null
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name: string
  category: string
  netWeight: number
  weightUnit: string
  rneNumber?: string | null
  rnpaNumber?: string | null
  servingSizeG?: number | null
  crossContamination?: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
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

export interface NutritionValues extends RoundedNutritionValues {
  // same fields but as doubles (raw, unrounded)
}

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
