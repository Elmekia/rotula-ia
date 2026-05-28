export interface Product {
  id: string
  tenantId: string
  name: string
  category: string
  netWeight: number
  weightUnit: string
  rneNumber: string | null
  rnpaNumber: string | null
  status: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name: string
  category: string
  netWeight: number
  weightUnit: string
  rneNumber?: string | null
  rnpaNumber?: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
