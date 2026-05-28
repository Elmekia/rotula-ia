export interface Ingredient {
  id: string
  productId: string
  tenantId: string
  name: string
  percentage: number
  allergen: boolean
  sortOrder: number
  createdAt: string
}

export interface IngredientRequest {
  name: string
  percentage: number
  allergen: boolean | null
  sortOrder: number
}
