import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Package, FileText, Tag } from 'lucide-react'
import clsx from 'clsx'

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/products',  icon: Package,         label: 'Productos'  },
  { to: '/labels',    icon: FileText,         label: 'Rótulos'    },
]

export function Sidebar() {
  return (
    <aside className="flex flex-col w-64 min-h-screen bg-slate-900 text-slate-200">
      {/* Logo */}
      <div className="flex items-center gap-2 px-6 py-5 border-b border-slate-700">
        <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-blue-600">
          <Tag className="w-4 h-4 text-white" />
        </div>
        <span className="text-lg font-bold text-white tracking-tight">RotulaIA</span>
      </div>

      {/* Navegación */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white',
              )
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Footer branding */}
      <div className="px-6 py-4 border-t border-slate-700">
        <p className="text-xs text-slate-500">RotulaIA v0.1.0</p>
      </div>
    </aside>
  )
}
