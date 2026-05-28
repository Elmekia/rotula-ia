import { useNavigate, useLocation } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuthStore } from '../../store/authStore'

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/products':  'Productos',
  '/labels':    'Rótulos',
}

export function Header() {
  const navigate  = useNavigate()
  const location  = useLocation()
  const { user, logout } = useAuthStore()

  // /products/:id → show "Productos" as title (detail page has its own breadcrumb)
  const isProductDetail = /^\/products\/[^/]+$/.test(location.pathname)
  const title = isProductDetail
    ? 'Productos'
    : (PAGE_TITLES[location.pathname] ?? 'RotulaIA')

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const initials = user?.email?.charAt(0).toUpperCase() ?? '?'

  return (
    <header className="flex items-center justify-between px-6 py-3 bg-white border-b border-slate-200">
      <h1 className="text-lg font-semibold text-slate-800">{title}</h1>

      <div className="flex items-center gap-3">
        {/* Avatar */}
        <div className="flex items-center gap-2">
          <div className="flex items-center justify-center w-8 h-8 rounded-full bg-blue-600 text-white text-sm font-semibold">
            {initials}
          </div>
          <span className="text-sm text-slate-600 hidden sm:block">{user?.email}</span>
        </div>

        {/* Logout */}
        <button
          onClick={handleLogout}
          title="Cerrar sesión"
          className="flex items-center gap-1.5 px-2.5 py-1.5 text-sm text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
        >
          <LogOut className="w-4 h-4" />
          <span className="hidden sm:block">Salir</span>
        </button>
      </div>
    </header>
  )
}
