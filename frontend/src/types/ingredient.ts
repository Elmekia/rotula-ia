export interface Ingredient {
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

export interface IngredientRequest {
  name: string
  /** Peso del ingrediente en gramos. */
  weightGrams: number
  allergen: boolean | null
}
