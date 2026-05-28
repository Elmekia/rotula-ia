import api from './api'
import type { PageResponse, Product, ProductRequest } from '../types/product'

export const productsApi = {
  list(page = 0, size = 20, sort = 'createdAt,desc') {
    return api
      .get<PageResponse<Product>>('/products', { params: { page, size, sort } })
      .then((r) => r.data)
  },

  getById(id: string) {
    return api.get<Product>(`/products/${id}`).then((r) => r.data)
  },

  create(data: ProductRequest) {
    return api.post<Product>('/products', data).then((r) => r.data)
  },

  update(id: string, data: ProductRequest) {
    return api.put<Product>(`/products/${id}`, data).then((r) => r.data)
  },

  delete(id: string) {
    return api.delete(`/products/${id}`)
  },
}
