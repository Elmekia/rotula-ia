import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, AlertTriangle } from 'lucide-react'
import { ingredientsApi } from '../../lib/ingredientsApi'
import { detectAllergen } from '../../lib/allergenDetector'
import { useToastStore } from '../../store/toastStore'
import { IngredientForm } from './IngredientForm'
import { DeleteModal } from '../products/DeleteModal'
import { SkeletonTable } from '../ui/Skeleton'
import type { Ingredient, IngredientRequest } from '../../types/ingredient'
import type { Product } from '../../types/product'

interface Props {
  product: Product
}

export function IngredientList({ product }: Props) {
  const qc = useQueryClient()
  const { addToast } = useToastStore()

  const [formOpen, setFormOpen]     = useState(false)
  const [editTarget, setEditTarget] = useState<Ingredient | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Ingredient | null>(null)

  // ── Query ────────────────────────────────────────────────────────────────
  const { data: ingredients = [], isLoading, isError } = useQuery({
    queryKey: ['ingredients', product.id],
    queryFn: () => ingredientsApi.list(product.id),
  })

  // ── Mutations ────────────────────────────────────────────────────────────
  const createMut = useMutation({
    mutationFn: (req: IngredientRequest) => ingredientsApi.create(product.id, req),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['ingredients', product.id] })
      setFormOpen(false)
      addToast('Ingrediente agregado')
    },
    onError(err: any) {
      const msg = err?.response?.data?.error ?? 'No se pudo agregar el ingrediente'
      addToast(msg, 'error')
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, req }: { id: string; req: IngredientRequest }) =>
      ingredientsApi.update(id, req),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['ingredients', product.id] })
      setFormOpen(false)
      setEditTarget(null)
      addToast('Ingrediente actualizado')
    },
    onError(err: any) {
      const msg = err?.response?.data?.error ?? 'No se pudo actualizar el ingrediente'
      addToast(msg, 'error')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => ingredientsApi.delete(id),
    onSuccess() {
      qc.invalidateQueries({ queryKey: ['ingredients', product.id] })
      setDeleteTarget(null)
      addToast('Ingrediente eliminado')
    },
    onError() {
      addToast('No se pudo eliminar el ingrediente', 'error')
    },
  })

  // ── Computed ──────────────────────────────────────────────────────────────
  const totalPercentage = ingredients.reduce((sum, i) => sum + i.percentage, 0)
  const isSubmitting    = createMut.isPending || updateMut.isPending

  // ── Handlers ──────────────────────────────────────────────────────────────
  function handleFormSubmit(req: IngredientRequest) {
    if (editTarget) {
      updateMut.mutate({ id: editTarget.id, req })
    } else {
      createMut.mutate(req)
    }
  }

  function handleOpenEdit(ingredient: Ingredient) {
    setEditTarget(ingredient)
    setFormOpen(true)
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="space-y-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold text-slate-700">Lista de ingredientes</h3>
          {ingredients.length > 0 && (
            <p className={`text-xs mt-0.5 ${totalPercentage > 100 ? 'text-red-500' : 'text-slate-400'}`}>
              Suma total: {totalPercentage.toFixed(3)}%
              {totalPercentage > 100 && ' · supera el 100%'}
            </p>
          )}
        </div>
        <button
          onClick={() => { setEditTarget(null); setFormOpen(true) }}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700
                     text-white text-xs font-medium rounded-lg transition-colors"
        >
          <Plus className="w-3.5 h-3.5" />
          Agregar ingrediente
        </button>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        {/* Column headers */}
        <div className="grid grid-cols-12 gap-2 px-4 py-2.5 border-b border-slate-100
                        text-xs font-semibold text-slate-500 uppercase tracking-wide">
          <span className="col-span-5">Nombre</span>
          <span className="col-span-3">Porcentaje</span>
          <span className="col-span-2">Alérgeno</span>
          <span className="col-span-2 text-right">Acciones</span>
        </div>

        {isLoading ? (
          <SkeletonTable rows={3} cols={5} />
        ) : isError ? (
          <p className="text-sm text-slate-500 text-center py-8">Error al cargar ingredientes</p>
        ) : ingredients.length === 0 ? (
          <div className="flex flex-col items-center py-10 text-slate-400">
            <p className="text-sm font-medium text-slate-600">Sin ingredientes</p>
            <p className="text-xs mt-1">Agregá el primero con el botón de arriba</p>
          </div>
        ) : (
          ingredients.map((ing) => (
            <div
              key={ing.id}
              className="grid grid-cols-12 gap-2 px-4 py-3 border-b border-slate-100 last:border-0
                         items-center hover:bg-slate-50 transition-colors"
            >
              {/* Name */}
              <div className="col-span-5">
                <span className="text-sm text-slate-800 truncate block">{ing.name}</span>
              </div>

              {/* Percentage */}
              <span className="col-span-3 text-sm font-medium text-slate-700">
                {ing.percentage.toFixed(3)}%
              </span>

              {/* Allergen badge — always re-detected from name per Res. 109/2023 */}
              <div className="col-span-2">
                {detectAllergen(ing.name) ? (
                  <span className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-amber-100
                                   text-amber-700 rounded text-xs font-medium">
                    <AlertTriangle className="w-3 h-3" />
                    Alérgeno
                  </span>
                ) : (
                  <span className="text-xs text-slate-400">—</span>
                )}
              </div>

              {/* Actions */}
              <div className="col-span-2 flex items-center justify-end gap-1">
                <button
                  onClick={() => handleOpenEdit(ing)}
                  className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                  title="Editar"
                >
                  <Pencil className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => setDeleteTarget(ing)}
                  className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                  title="Eliminar"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Modals */}
      {formOpen && (
        <IngredientForm
          ingredient={editTarget}
          isSubmitting={isSubmitting}
          onSubmit={handleFormSubmit}
          onClose={() => { setFormOpen(false); setEditTarget(null) }}
        />
      )}

      {deleteTarget && (
        <DeleteModal
          product={deleteTarget as any}
          itemLabel="ingrediente"
          isDeleting={deleteMut.isPending}
          onConfirm={() => deleteMut.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
