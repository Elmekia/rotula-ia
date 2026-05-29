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

  /**
   * Descarga el PDF del rótulo con las dimensiones indicadas.
   * Devuelve una URL de objeto Blob para disparar la descarga en el navegador.
   */
  async exportPdf(
    labelId: string,
    widthCm = 10,
    heightCm = 15,
  ): Promise<{ url: string; filename: string }> {
    const response = await api.get(`/labels/${labelId}/export`, {
      params: { widthCm, heightCm },
      responseType: 'blob',
    })
    const disposition: string = response.headers['content-disposition'] ?? ''
    const match = disposition.match(/filename="([^"]+)"/)
    const filename = match ? match[1] : `rotulo_${labelId}.pdf`
    const url = URL.createObjectURL(response.data as Blob)
    return { url, filename }
  },
}
