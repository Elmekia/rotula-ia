import { Plus, Package } from 'lucide-react'

export function Products() {
  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">Gestioná tu catálogo de productos</p>
        <button
          className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700
                     text-white text-sm font-medium rounded-lg transition-colors"
          disabled
          title="Próximamente"
        >
          <Plus className="w-4 h-4" />
          Nuevo producto
        </button>
      </div>

      {/* Empty state */}
      <div className="bg-white rounded-xl border border-slate-200">
        {/* Table header */}
        <div className="grid grid-cols-4 gap-4 px-6 py-3 border-b border-slate-100 text-xs font-semibold text-slate-500 uppercase tracking-wide">
          <span>Nombre</span>
          <span>Categoría</span>
          <span>Peso neto</span>
          <span>Estado</span>
        </div>

        <div className="flex flex-col items-center py-16 text-slate-400">
          <Package className="w-10 h-10 mb-3 opacity-30" />
          <p className="text-sm font-medium text-slate-600">No hay productos todavía</p>
          <p className="text-xs mt-1">Los productos que agregues aparecerán aquí</p>
        </div>
      </div>
    </div>
  )
}
