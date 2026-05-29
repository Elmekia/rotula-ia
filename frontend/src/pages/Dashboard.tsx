import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Package, FileText, CheckCircle, Clock, Plus, Loader2, AlertTriangle,
} from 'lucide-react'
import { dashboardApi } from '../lib/dashboardApi'
import type { RecentLabelEntry } from '../types/dashboard'

const STATUS_COLORS: Record<string, string> = {
  draft:    'bg-slate-100 text-slate-600',
  approved: 'bg-emerald-100 text-emerald-700',
  exported: 'bg-blue-100 text-blue-700',
}
const STATUS_LABELS: Record<string, string> = {
  draft: 'Borrador', approved: 'Aprobado', exported: 'Exportado',
}

export function Dashboard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: dashboardApi.getSummary,
    staleTime: 30_000,
  })

  const stats = [
    {
      label: 'Productos',
      value: data?.totalProducts ?? '—',
      icon: Package,
      color: 'bg-blue-50 text-blue-600',
    },
    {
      label: 'Rótulos',
      value: data?.totalLabels ?? '—',
      icon: FileText,
      color: 'bg-violet-50 text-violet-600',
    },
    {
      label: 'Aprobados',
      value: data?.approvedLabels ?? '—',
      icon: CheckCircle,
      color: 'bg-green-50 text-green-600',
    },
    {
      label: 'En revisión',
      value: data?.pendingReviewLabels ?? '—',
      icon: Clock,
      color: 'bg-amber-50 text-amber-600',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header row */}
      <div className="flex items-center justify-between">
        <p className="text-slate-500 text-sm">
          Bienvenido a RotulaIA. Desde aquí podés gestionar tus productos y rótulos.
        </p>
        <Link
          to="/labels/new"
          className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700
                     text-white text-sm font-semibold rounded-lg transition-colors shadow-sm"
        >
          <Plus className="w-4 h-4" />
          Nuevo rótulo
        </Link>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div
            key={label}
            className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4"
          >
            <div className={`p-2.5 rounded-lg ${color}`}>
              <Icon className="w-5 h-5" />
            </div>
            <div>
              {isLoading ? (
                <Loader2 className="w-5 h-5 animate-spin text-slate-300 mb-0.5" />
              ) : (
                <p className="text-2xl font-bold text-slate-800">{value}</p>
              )}
              <p className="text-xs text-slate-500 mt-0.5">{label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Rótulos recientes */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-slate-700">Últimos rótulos generados</h2>
          <Link to="/labels" className="text-xs text-blue-500 hover:underline">
            Ver todos →
          </Link>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-slate-400">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Cargando…</span>
          </div>
        ) : isError ? (
          <div className="flex items-center gap-2 py-8 text-slate-400 justify-center">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            <span className="text-sm">No se pudo cargar la actividad reciente.</span>
          </div>
        ) : data && data.recentLabels.length > 0 ? (
          <div className="space-y-2">
            {data.recentLabels.map(entry => (
              <RecentRow key={entry.labelId} entry={entry} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center py-12 text-slate-400">
            <Clock className="w-8 h-8 mb-2 opacity-40" />
            <p className="text-sm">Sin actividad reciente</p>
            <Link
              to="/labels/new"
              className="mt-3 text-xs text-blue-500 hover:underline font-medium"
            >
              Generá tu primer rótulo →
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Fila de rótulo reciente ───────────────────────────────────────────────────

function RecentRow({ entry }: { entry: RecentLabelEntry }) {
  return (
    <Link
      to="/labels"
      className="flex items-center justify-between px-4 py-3 rounded-lg border border-slate-200
                 hover:bg-slate-50 transition-colors group"
    >
      {/* Producto + denominación */}
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-slate-800 truncate group-hover:text-blue-600 transition-colors">
          {entry.productName}
        </p>
        <p className="text-xs text-slate-400 truncate mt-0.5">
          {entry.legalDenomination ?? 'Sin denominación legal'}
        </p>
      </div>

      {/* Versión */}
      <span className="mx-4 text-xs font-bold text-slate-400 shrink-0">
        v{entry.version}
      </span>

      {/* Fecha */}
      <span className="mx-4 text-xs text-slate-400 shrink-0 hidden sm:block">
        {new Date(entry.generatedAt).toLocaleString('es-AR', {
          day: '2-digit', month: '2-digit', year: 'numeric',
          hour: '2-digit', minute: '2-digit',
        })}
      </span>

      {/* Estado */}
      <span
        className={`px-2 py-0.5 text-xs font-medium rounded-full shrink-0
          ${STATUS_COLORS[entry.status] ?? 'bg-slate-100 text-slate-600'}`}
      >
        {STATUS_LABELS[entry.status] ?? entry.status}
      </span>
    </Link>
  )
}
