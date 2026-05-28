import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface AuthUser {
  id: string
  email: string
  role: string
  tenantId: string
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  login: (accessToken: string, refreshToken: string) => void
  logout: () => void
}

/** Decodifica el payload del JWT sin verificar firma (solo lectura de claims). */
function parseJwtPayload(token: string): Record<string, unknown> {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(window.atob(base64))
  } catch {
    return {}
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      login(accessToken, refreshToken) {
        const p = parseJwtPayload(accessToken)
        const user: AuthUser = {
          id:       p.sub       as string,
          email:    p.email     as string,
          role:     p.role      as string,
          tenantId: p.tenant_id as string,
        }
        set({ accessToken, refreshToken, user, isAuthenticated: true })
      },

      logout() {
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false })
      },
    }),
    { name: 'rotula-auth' },
  ),
)
