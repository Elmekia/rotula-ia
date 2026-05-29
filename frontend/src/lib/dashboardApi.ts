import api from './api'
import type { DashboardSummary } from '../types/dashboard'

export const dashboardApi = {
  /** Devuelve el resumen del dashboard para el tenant del usuario autenticado. */
  getSummary(): Promise<DashboardSummary> {
    return api.get<DashboardSummary>('/dashboard/summary').then(r => r.data)
  },
}
