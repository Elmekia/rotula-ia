import { AlertTriangle } from 'lucide-react'
import type { Product } from '../../types/product'

interface Props {
  product: Product
  /** Etiqueta del tipo de ítem. Por defecto: "producto". */
  itemLabel?: string
  isDeleting: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function DeleteModal({ product, itemLabel = 'producto', isDeleting, onConfirm, onCancel }: Props) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
        <div className="flex flex-col items-center text-center gap-3">
          <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6 text-red-600" />
          </div>
          <h2 className="text-lg font-semibold text-slate-800">Eliminar {itemLabel}</h2>
          <p className="text-sm text-slate-500">
            ¿Estás seguro que querés eliminar{' '}
            <span className="font-medium text-slate-700">"{product.name}"</span>?
            Esta acción no se puede deshacer.
          </p>
        </div>

        <div className="flex gap-3 mt-6">
          <button
            onClick={onCancel}
            disabled={isDeleting}
            className="flex-1 px-4 py-2 text-sm font-medium text-slate-600 border border-slate-200
                       rounded-lg hover:bg-slate-50 disabled:opacity-60 transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            disabled={isDeleting}
            className="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600
                       hover:bg-red-700 disabled:opacity-60 rounded-lg transition-colors
                       flex items-center justify-center gap-2"
          >
            {isDeleting && (
              <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            )}
            Eliminar
          </button>
        </div>
      </div>
    </div>
  )
}
