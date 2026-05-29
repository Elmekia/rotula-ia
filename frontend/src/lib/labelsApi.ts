import api from './api'
import type { LabelVersionResponse, LabelVersionSummary } from '../types/label'

export const labelsApi = {
  /** Genera una nueva versión de rótulo para el producto. */
  generate(productId: string): Promise<LabelVersionResponse> {
    return api.post<LabelVersionResponse>('/labels/generate', { productId }).then(r => r.data)
  },

  /** Devuelve el historial de versiones de un producto, más reciente primero. */
  getVersions(productId: string): Promise<LabelVersionSummary[]> {
    return api.get<LabelVersionSummary[]>(`/labels/${productId}/versions`).then(r => r.data)
  },
}
