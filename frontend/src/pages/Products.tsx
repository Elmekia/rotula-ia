import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Package, ChevronLeft, ChevronRight } from 'lucide-react'
import { productsApi } from '../lib/productsApi'
import { useToastStore } from '../store/toastStore'
import { ProductTable } from '../components/products/ProductTable'
import { ProductForm } from '../components/products/ProductForm'
import { DeleteModal } from '../components/products/DeleteModal'
import { SkeletonTable } from '../components/ui/Skeleton'
import type { Product, ProductRequest } from '../types/product'

const PAGE_SIZE = 20

export function Products() {
  const qc = useQueryClient()
  const { addToast } = useToastStore()

  // ── Pagination ───────────────────────────────────────────────────────────
  const [page, setPage] = useState(0)

  // ── Modal state ──────────────────────────────────────────────────────────
  const [formOpen, setFormOpen]     = useState(false)
  const [editTarget, setEditTarget] = useState<Product | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null)

  // ── Query ────────────────────────────────────────────────────────────────
  const { data, isLoading, isError } = useQuery({
    queryKey: ['products', page],
    queryFn: () => productsApi.list(page, PAGE_SIZE),
    placeholderData: (prev) => prev,
  })

  // ── Mutations ────────────────────────────────────────────────────────────
  const createMut = useMutation({
    mutationFn: (req: ProductRequest) => productsApi.create(req),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['products'] })
      setFormOpen(false)
      addToast('Producto creado correctamente')
    },
    onError() {
      addToast('No se pudo crear el producto', 'error')
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, req }: { id: string; req: ProductRequest }) =>
      productsApi.update(id, req),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['products'] })
      setFormOpen(false)
      setEditTarget(null)
      addToast('Producto actualizado correctamente')
    },
    onError() {
      addToast('No se pudo actualizar el producto', 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => productsApi.delete(id),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['products'] })
      setDeleteTarget(null)
      addToast('Producto eliminado')
    },
    onError() {
      addToast('No se pudo eliminar el producto', 'error')
    },
  })

  // ── Handlers ─────────────────────────────────────────────────────────────
  function handleOpenCreate() {
    setEditTarget(null)
    setFormOpen(true)
  }

  function handleOpenEdit(product: Product) {
    setEditTarget(product)
    setFormOpen(true)
  }

  function handleFormSubmit(req: ProductRequest) {
    if (editTarget) {
      updateMut.mutate({ id: editTarget.id, req })
    } else {
      createMut.mutate(req)
    }
  }

  function handleCloseForm() {
    setFormOpen(false)
    setEditTarget(null)
  }

  const isSubmitting = createMut.isPending || updateMut.isPending

  // ── Render ───────────────────────────────────────────────────────────────
  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">Gestioná tu catálogo de productos</p>
        <button
          onClick={handleOpenCreate}
          className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700
                     text-white text-sm font-medium rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          Nuevo producto
        </button>
      </div>

      {/* Table card */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        {isLoading ? (
          <>
            {/* Skeleton header */}
            <div className="grid grid-cols-5 gap-4 px-6 py-3 border-b border-slate-100
                            text-xs font-semibold text-slate-500 uppercase tracking-wide">
              <span className="col-span-2">Nombre</span>
              <span>Categoría</span>
              <span>Peso neto</span>
              <span>Estado</span>
            </div>
            <SkeletonTable rows={5} cols={5} />
          </>
        ) : isError ? (
          <div className="flex flex-col items-center py-16 text-slate-400">
            <Package className="w-10 h-10 mb-3 opacity-30" />
            <p className="text-sm font-medium text-slate-600">Error al cargar los productos</p>
            <p className="text-xs mt-1">Verificá tu conexión e intentá de nuevo</p>
          </div>
        ) : data && data.content.length > 0 ? (
          <>
            <ProductTable
              products={data.content}
              onEdit={handleOpenEdit}
              onDelete={(p) => setDeleteTarget(p)}
            />
            {/* Pagination */}
            {data.totalPages > 1 && (
              <div className="flex items-center justify-between px-6 py-3 border-t border-slate-100">
                <p className="text-xs text-slate-500">
                  {data.totalElements} producto{data.totalElements !== 1 ? 's' : ''} en total
                </p>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setPage((p) => p - 1)}
                    disabled={page === 0}
                    className="p-1.5 rounded text-slate-400 hover:text-slate-700 disabled:opacity-40
                               hover:bg-slate-100 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <span className="text-xs text-slate-600">
                    Pág. {page + 1} / {data.totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => p + 1)}
                    disabled={data.last}
                    className="p-1.5 rounded text-slate-400 hover:text-slate-700 disabled:opacity-40
                               hover:bg-slate-100 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          /* Empty state */
          <div className="flex flex-col items-center py-16 text-slate-400">
            <Package className="w-10 h-10 mb-3 opacity-30" />
            <p className="text-sm font-medium text-slate-600">No hay productos todavía</p>
            <p className="text-xs mt-1">
              Hacé clic en{' '}
              <button onClick={handleOpenCreate} className="text-blue-500 hover:underline">
                Nuevo producto
              </button>{' '}
              para agregar el primero
            </p>
          </div>
        )}
      </div>

      {/* Modals */}
      {formOpen && (
        <ProductForm
          product={editTarget}
          isSubmitting={isSubmitting}
          onSubmit={handleFormSubmit}
          onClose={handleCloseForm}
        />
      )}

      {deleteTarget && (
        <DeleteModal
          product={deleteTarget}
          isDeleting={deleteMut.isPending}
          onConfirm={() => deleteMut.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
