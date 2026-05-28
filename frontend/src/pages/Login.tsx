import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Tag, Loader2 } from 'lucide-react'
import axios from 'axios'
import { useAuthStore } from '../store/authStore'

// ── Schema de validación ──────────────────────────────────────
const loginSchema = z.object({
  email:    z.string().email('Email inválido'),
  password: z.string().min(1, 'La contraseña es requerida'),
})
type LoginForm = z.infer<typeof loginSchema>

// ── Componente ────────────────────────────────────────────────
export function Login() {
  const navigate        = useNavigate()
  const { login, isAuthenticated } = useAuthStore()

  // Si ya hay sesión, redirigir directo al dashboard
  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true })
  }, [isAuthenticated, navigate])

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  const mutation = useMutation({
    mutationFn: async (data: LoginForm) => {
      const res = await axios.post('/api/auth/login', data)
      return res.data as { access_token: string; refresh_token: string }
    },
    onSuccess({ access_token, refresh_token }) {
      login(access_token, refresh_token)
      navigate('/dashboard', { replace: true })
    },
    onError(err) {
      const msg =
        axios.isAxiosError(err) && err.response?.status === 401
          ? 'Email o contraseña incorrectos'
          : 'Error al iniciar sesión. Intentá de nuevo.'
      setError('root', { message: msg })
    },
  })

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 p-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="flex flex-col items-center mb-8">
          <div className="flex items-center justify-center w-12 h-12 rounded-2xl bg-blue-600 mb-3">
            <Tag className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">RotulaIA</h1>
          <p className="text-slate-400 text-sm mt-1">Rotulado alimenticio inteligente</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <h2 className="text-xl font-semibold text-slate-800 mb-6">Iniciar sesión</h2>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} noValidate className="space-y-4">
            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="tu@empresa.com"
                {...register('email')}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-slate-900
                           placeholder:text-slate-400 text-sm
                           focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                           disabled:opacity-60"
                disabled={mutation.isPending}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1" htmlFor="password">
                Contraseña
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                {...register('password')}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 text-slate-900
                           placeholder:text-slate-400 text-sm
                           focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                           disabled:opacity-60"
                disabled={mutation.isPending}
              />
              {errors.password && (
                <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>
              )}
            </div>

            {/* Error global */}
            {errors.root && (
              <div className="px-3.5 py-2.5 rounded-lg bg-red-50 border border-red-200">
                <p className="text-sm text-red-700">{errors.root.message}</p>
              </div>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={mutation.isPending}
              className="w-full flex items-center justify-center gap-2 px-4 py-2.5 mt-2
                         bg-blue-600 hover:bg-blue-700 active:bg-blue-800
                         text-white font-medium text-sm rounded-lg
                         transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {mutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              {mutation.isPending ? 'Ingresando…' : 'Ingresar'}
            </button>
          </form>
        </div>

        <p className="text-center text-slate-500 text-xs mt-6">
          © {new Date().getFullYear()} RotulaIA — Todos los derechos reservados
        </p>
      </div>
    </div>
  )
}
