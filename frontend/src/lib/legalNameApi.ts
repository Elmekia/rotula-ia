import api from './api'
import type { LegalNameSuggestion } from '../types/label'

export const legalNameApi = {
  /** Obtiene la sugerencia de denominación legal para el producto. */
  getSuggestion(productId: string): Promise<LegalNameSuggestion> {
    return api.get<LegalNameSuggestion>(`/products/${productId}/legal-name`).then(r => r.data)
  },
}
