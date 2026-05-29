/** Entrada en la lista de rótulos recientes del dashboard. */
export interface RecentLabelEntry {
  labelId: string
  productId: string
  productName: string
  version: number
  status: 'draft' | 'approved' | 'exported'
  legalDenomination: string | null
  generatedAt: string
}

/** Respuesta de GET /dashboard/summary */
export interface DashboardSummary {
  totalProducts: number
  totalLabels: number
  pendingReviewLabels: number
  approvedLabels: number
  recentLabels: RecentLabelEntry[]
}
