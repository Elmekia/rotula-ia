import api from './api'
import type { Ingredient, IngredientRequest } from '../types/ingredient'

export const ingredientsApi = {
  list(productId: string) {
    return api.get<Ingredient[]>(`/products/${productId}/ingredients`).then((r) => r.data)
  },

  create(productId: string, data: IngredientRequest) {
    return api.post<Ingredient>(`/products/${productId}/ingredients`, data).then((r) => r.data)
  },

  update(id: string, data: IngredientRequest) {
    return api.put<Ingredient>(`/ingredients/${id}`, data).then((r) => r.data)
  },

  delete(id: string) {
    return api.delete(`/ingredients/${id}`)
  },
}
