/** Campos nutricionales por 100 g del ingrediente (todos opcionales). */
export interface IngredientNutrition {
  energyKcalPer100g: number | null
  proteinsPer100g:   number | null
  carbsPer100g:      number | null
  sugarsPer100g:     number | null
  fatTotalPer100g:   number | null
  fatSatPer100g:     number | null
  fatTransPer100g:   number | null
  sodiumMgPer100g:   number | null
}

export interface Ingredient extends IngredientNutrition {
  id: string
  productId: string
  tenantId: string
  name: string
  /** Peso ingresado por el usuario en gramos. */
  weightGrams: number
  /** Porcentaje calculado automáticamente: weightGrams / sum(weightGrams) * 100 */
  percentage: number
  allergen: boolean
  createdAt: string
}

export interface IngredientRequest extends Partial<IngredientNutrition> {
  name: string
  /** Peso del ingrediente en gramos. */
  weightGrams: number
  allergen: boolean | null
}
