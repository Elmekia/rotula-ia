import api from './api'
import type { FoodGroup, FoodItem } from '../types/product'

export const foodReferenceApi = {
  /** Lista todos los grupos de alimentos activos (TABLA I). */
  listGroups(): Promise<FoodGroup[]> {
    return api.get<FoodGroup[]>('/food-reference/groups').then((r) => r.data)
  },

  /** Lista los alimentos activos de un grupo con sus porciones de referencia. */
  listItems(groupId: string): Promise<FoodItem[]> {
    return api.get<FoodItem[]>(`/food-reference/groups/${groupId}/items`).then((r) => r.data)
  },

  /** Obtiene un alimento específico por id (para pre-cargar al editar). */
  getItem(itemId: string): Promise<FoodItem> {
    return api.get<FoodItem>(`/food-reference/items/${itemId}`).then((r) => r.data)
  },
}
