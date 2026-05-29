import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus, FileText, ChevronDown, ChevronUp, Loader2, Tag, Download } from 'lucide-react'
import { productsApi } from '../lib/productsApi'
import { labelsApi } from '../lib/labelsApi'
import type { Product } from '../types/product'
import type { LabelVersionSummary } from '../types/label'

export function Labels() {
  const [expandedProductId, setExpandedProductId] = useState<string | null>(null)

  const { data: products = [], isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => productsApi.list(0, 100).then(p => p.content),
  })

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">Creá y gestioná los rótulos de tus productos</p>
        <Link
          to="/labels/new"
          className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700
                     text-white text-sm font-medium rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          Nuevo rótulo
        </Link>
      </div>

      {/* Lista de productos con sus versiones */}
      <div className="bg-white rounded-xl border border-slate-200">
        {/* Table header */}
        <div className="grid grid-cols-4 gap-4 px-6 py-3 border-b border-slate-100
                        text-xs font-semibold text-slate-500 uppercase tracking-wide">
          <span>Producto</span>
          <span>Categoría</span>
          <span>Rótulos generados</span>
          <span className="text-right">Acciones</span>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center gap-2 py-12 text-slate-400">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Cargando productos…</span>
          </div>
        ) : products.length === 0 ? (
          <div className="flex flex-col items-center py-16 text-slate-400">
            <FileText className="w-10 h-10 mb-3 opacity-30" />
            <p className="text-sm font-medium text-slate-600">No hay productos todavía</p>
            <Link to="/products" className="text-xs text-blue-500 hover:underline mt-1">
              Creá tu primer producto →
            </Link>
          </div>
        ) : (
          products.map(product => (
            <ProductRow
              key={product.id}
              product={product}
              expanded={expandedProductId === product.id}
              onToggle={() =>
                setExpandedProductId(prev => prev === product.id ? null : product.id)
              }
            />
          ))
        )}
      </div>
    </div>
  )
}

// ── Fila de producto con historial de versiones ───────────────────────────────

function ProductRow({
  product, expanded, onToggle,
}: {
  product: Product
  expanded: boolean
  onToggle: () => void
}) {
  const { data: versions = [], isLoading } = useQuery({
    queryKey: ['label-versions', product.id],
    queryFn: () => labelsApi.getVersions(product.id),
    enabled: expanded,
  })

  return (
    <>
      <div
        className="grid grid-cols-4 gap-4 px-6 py-4 border-b border-slate-100 last:border-0
                   items-center hover:bg-slate-50 transition-colors cursor-pointer"
        onClick={onToggle}
      >
        {/* Nombre */}
        <div>
          <p className="text-sm font-semibold text-slate-800 truncate">{product.name}</p>
          <p className="text-xs text-slate-400 mt-0.5">{product.netWeight} {product.weightUnit}</p>
        </div>

        {/* Categoría */}
        <p className="text-sm text-slate-600 capitalize">{product.category}</p>

        {/* Contador de rótulos */}
        <div className="flex items-center gap-1.5">
          <Tag className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-sm text-slate-600">
            {expanded && !isLoading
              ? `${versions.length} versión${versions.length !== 1 ? 'es' : ''}`
              : '—'}
          </span>
        </div>

        {/* Acciones */}
        <div className="flex items-center justify-end gap-2">
          <Link
            to="/labels/new"
            onClick={e => e.stopPropagation()}
            className="px-3 py-1.5 text-xs font-medium text-blue-600 border border-blue-200
                       rounded-lg hover:bg-blue-50 transition-colors"
          >
            + Generar
          </Link>
          {expanded
            ? <ChevronUp className="w-4 h-4 text-slate-400" />
            : <ChevronDown className="w-4 h-4 text-slate-400" />}
        </div>
      </div>

      {/* Versiones expandidas */}
      {expanded && (
        <div className="border-b border-slate-100 bg-slate-50">
          {isLoading ? (
            <div className="flex items-center gap-2 px-10 py-4 text-slate-400 text-sm">
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
              Cargando versiones…
            </div>
          ) : versions.length === 0 ? (
            <p className="px-10 py-4 text-sm text-slate-400">
              No hay rótulos generados para este producto.{' '}
              <Link to="/labels/new" className="text-blue-500 hover:underline">Generar el primero →</Link>
            </p>
          ) : (
            <div className="px-10 py-3 space-y-2">
              {versions.map(v => (
                <VersionRow key={v.id} version={v} />
              ))}
            </div>
          )}
        </div>
      )}
    </>
  )
}

// ── Fila de versión ───────────────────────────────────────────────────────────

const STATUS_COLORS: Record<string, string> = {
  draft:    'bg-slate-100 text-slate-600',
  approved: 'bg-emerald-100 text-emerald-700',
  exported: 'bg-blue-100 text-blue-700',
}
const STATUS_LABELS: Record<string, string> = {
  draft: 'Borrador', approved: 'Aprobado', exported: 'Exportado',
}

function VersionRow({ version }: { version: LabelVersionSummary }) {
  const [downloading, setDownloading] = useState(false)

  async function handleExport(e: React.MouseEvent) {
    e.stopPropagation()
    setDownloading(true)
    try {
      const { url, filename } = await labelsApi.exportPdf(version.id)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="flex items-center justify-between py-2 px-3 bg-white rounded-lg border border-slate-200">
      <div className="flex items-center gap-3">
        <span className="text-xs font-bold text-slate-500 w-6 text-center">v{version.version}</span>
        <div>
          <p className="text-sm font-medium text-slate-700">
            {version.legalDenomination ?? 'Sin denominación legal'}
          </p>
          <p className="text-xs text-slate-400">
            {new Date(version.generatedAt).toLocaleString('es-AR', {
              day: '2-digit', month: '2-digit', year: 'numeric',
              hour: '2-digit', minute: '2-digit',
            })}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${STATUS_COLORS[version.status] ?? ''}`}>
          {STATUS_LABELS[version.status] ?? version.status}
        </span>

        <button
          onClick={handleExport}
          disabled={downloading}
          title="Descargar PDF"
          className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-slate-600
                     border border-slate-200 rounded-lg hover:bg-slate-100 transition-colors
                     disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {downloading
            ? <Loader2 className="w-3 h-3 animate-spin" />
            : <Download className="w-3 h-3" />}
          PDF
        </button>
      </div>
    </div>
  )
}
