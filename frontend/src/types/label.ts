import type { AllergenDeclarationResult, NutritionCalculationResult } from './product'

/** Sugerencia de denominación legal por categoría (CAA Art. 1385). */
export interface LegalNameSuggestion {
  suggestedName: string | null
  modifiers: string[]
  alertDiffers: boolean
  sourceArticle: string | null
}

/** Ingrediente simplificado para el rótulo. */
export interface IngredientItem {
  name: string
  weightGrams: number
  percentage: number
  allergen: boolean
}

/** Rótulo completo estructurado (snapshot inmutable). */
export interface LabelDTO {
  productId: string
  productName: string
  legalDenomination: string | null
  category: string
  netWeight: number
  weightUnit: string
  rneNumber: string | null
  rnpaNumber: string | null
  ingredients: IngredientItem[]
  nutrition: NutritionCalculationResult | null
  seals: string[]
  allergens: AllergenDeclarationResult
  claims: string[]
  legalNameAlert: boolean
}

/** Respuesta de POST /labels/generate — versión completa con datos. */
export interface LabelVersionResponse {
  id: string
  productId: string
  version: number
  status: 'draft' | 'approved' | 'exported'
  labelData: LabelDTO
  generatedAt: string
}

/** Resumen de versión para el historial. */
export interface LabelVersionSummary {
  id: string
  productId: string
  version: number
  status: 'draft' | 'approved' | 'exported'
  legalDenomination: string | null
  generatedAt: string
}
