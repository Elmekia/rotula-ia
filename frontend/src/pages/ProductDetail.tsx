import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ChevronLeft, Package } from 'lucide-react'
import { productsApi } from '../lib/productsApi'
import { IngredientList } from '../components/ingredients/IngredientList'

export function ProductDetail() {
  const { id } = useParams<{ id: string }>()

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-5 w-32 bg-slate-200 rounded animate-pulse" />
        <div className="h-32 bg-white rounded-xl border border-slate-200 animate-pulse" />
      </div>
    )
  }

  if (isError || !product) {
    return (
      <div className="flex flex-col items-center py-20 text-slate-400">
        <Package className="w-10 h-10 mb-3 opacity-30" />
        <p className="text-sm font-medium text-slate-600">Producto no encontrado</p>
        <Link to="/products" className="text-xs text-blue-500 hover:underline mt-2">
          ← Volver a Productos
        </Link>
      </div>
    )
  }

  const STATUS_LABELS: Record<string, string> = {
    draft:    'Borrador',
    active:   'Activo',
    archived: 'Archivado',
  }

  return (
    <div className="space-y-6">
      {/* Breadcrumb */}
      <Link
        to="/products"
        className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 transition-colors"
      >
        <ChevronLeft className="w-4 h-4" />
        Productos
      </Link>

      {/* Product info card */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-slate-800">{product.name}</h2>
            <p className="text-sm text-slate-500 mt-0.5">{product.category}</p>
          </div>
          <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-slate-100 text-slate-600 shrink-0">
            {STATUS_LABELS[product.status] ?? product.status}
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-5 pt-5 border-t border-slate-100">
          <InfoCell label="Peso neto" value={`${product.netWeight} ${product.weightUnit}`} />
          <InfoCell label="RNE" value={product.rneNumber ?? '—'} />
          <InfoCell label="RNPA" value={product.rnpaNumber ?? '—'} />
          <InfoCell label="Estado" value={STATUS_LABELS[product.status] ?? product.status} />
        </div>
      </div>

      {/* Ingredients section */}
      <IngredientList product={product} />
    </div>
  )
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-slate-400 uppercase tracking-wide font-medium">{label}</p>
      <p className="text-sm text-slate-700 mt-0.5 font-medium">{value}</p>
    </div>
  )
}
