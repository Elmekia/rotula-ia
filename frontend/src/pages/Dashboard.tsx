import { Package, FileText, CheckCircle, Clock } from 'lucide-react'

const stats = [
  { label: 'Productos',        value: '—', icon: Package,     color: 'bg-blue-50 text-blue-600'   },
  { label: 'Rótulos',          value: '—', icon: FileText,    color: 'bg-violet-50 text-violet-600' },
  { label: 'Aprobados',        value: '—', icon: CheckCircle, color: 'bg-green-50 text-green-600'  },
  { label: 'En revisión',      value: '—', icon: Clock,       color: 'bg-amber-50 text-amber-600'  },
]

export function Dashboard() {
  return (
    <div className="space-y-6">
      <p className="text-slate-500 text-sm">
        Bienvenido a RotulaIA. Desde aquí podés gestionar tus productos y rótulos.
      </p>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon, color }) => (
          <div key={label} className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4">
            <div className={`p-2.5 rounded-lg ${color}`}>
              <Icon className="w-5 h-5" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-800">{value}</p>
              <p className="text-xs text-slate-500 mt-0.5">{label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Placeholder actividad reciente */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-sm font-semibold text-slate-700 mb-4">Actividad reciente</h2>
        <div className="flex flex-col items-center py-12 text-slate-400">
          <Clock className="w-8 h-8 mb-2 opacity-40" />
          <p className="text-sm">Sin actividad reciente</p>
        </div>
      </div>
    </div>
  )
}
