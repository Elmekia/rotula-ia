import { Pencil, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import clsx from 'clsx'
import type { Product } from '../../types/product'

const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  draft:     { label: 'Borrador',  className: 'bg-slate-100  text-slate-600' },
  active:    { label: 'Activo',    className: 'bg-emerald-100 text-emerald-700' },
  archived:  { label: 'Archivado', className: 'bg-amber-100  text-amber-700' },
}

interface Props {
  products: Product[]
  onEdit:   (product: Product) => void
  onDelete: (product: Product) => void
}

export function ProductTable({ products, onEdit, onDelete }: Props) {
  return (
    <div className="overflow-x-auto">
      {/* Header */}
      <div className="grid grid-cols-5 gap-4 px-6 py-3 border-b border-slate-100
                      text-xs font-semibold text-slate-500 uppercase tracking-wide min-w-[640px]">
        <span className="col-span-2">Nombre</span>
        <span>Categoría</span>
        <span>Peso neto</span>
        <span>Estado</span>
      </div>

      {/* Rows */}
      {products.map((p) => {
        const statusMeta = STATUS_LABELS[p.status] ?? { label: p.status, className: 'bg-slate-100 text-slate-600' }
        return (
          <div
            key={p.id}
            className="grid grid-cols-5 gap-4 px-6 py-4 border-b border-slate-100 last:border-0
                       items-center hover:bg-slate-50 transition-colors min-w-[640px]"
          >
            {/* Nombre → link a detalle */}
            <div className="col-span-2">
              <Link
                to={`/products/${p.id}`}
                className="font-medium text-slate-800 hover:text-blue-600 truncate block transition-colors"
              >
                {p.name}
              </Link>
              {p.rnpaNumber && (
                <p className="text-xs text-slate-400 mt-0.5">RNPA: {p.rnpaNumber}</p>
              )}
            </div>

            {/* Categoría */}
            <span className="text-sm text-slate-600 truncate">{p.category}</span>

            {/* Peso neto */}
            <span className="text-sm text-slate-600">
              {p.netWeight} {p.weightUnit}
            </span>

            {/* Estado + acciones */}
            <div className="flex items-center justify-between gap-2">
              <span className={clsx('px-2 py-0.5 rounded-full text-xs font-medium', statusMeta.className)}>
                {statusMeta.label}
              </span>
              <div className="flex gap-1 shrink-0">
                <button
                  onClick={() => onEdit(p)}
                  className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50
                             rounded transition-colors"
                  title="Editar"
                >
                  <Pencil className="w-4 h-4" />
                </button>
                <button
                  onClick={() => onDelete(p)}
                  className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50
                             rounded transition-colors"
                  title="Eliminar"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
