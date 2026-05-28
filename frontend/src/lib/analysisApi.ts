import api from './api'
import type { LabelAnalysisResult } from '../types/product'

export const analysisApi = {
  analyze(productId: string) {
    return api.get<LabelAnalysisResult>(`/products/${productId}/analysis`).then((r) => r.data)
  },
}
