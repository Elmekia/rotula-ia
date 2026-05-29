import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { AppLayout } from './components/layout/AppLayout'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Products } from './pages/Products'
import { ProductDetail } from './pages/ProductDetail'
import { Labels } from './pages/Labels'
import { LabelWizard } from './pages/LabelWizard'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Ruta pública */}
        <Route path="/login" element={<Login />} />

        {/* Rutas protegidas — redirigen a /login si no hay token */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard"       element={<Dashboard />} />
            <Route path="/products"        element={<Products />} />
            <Route path="/products/:id"    element={<ProductDetail />} />
            <Route path="/labels"          element={<Labels />} />
            <Route path="/labels/new"      element={<LabelWizard />} />
          </Route>
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
