import { Check } from 'lucide-react'
import type { WizardStep } from '../../store/wizardStore'

const STEPS: { label: string; description: string }[] = [
  { label: 'Producto',         description: 'Seleccioná el producto' },
  { label: 'Ingredientes',     description: 'Revisá la lista' },
  { label: 'Nutrición',        description: 'Completá los datos' },
  { label: 'Revisión',         description: 'Preview y generar' },
]

interface Props {
  current: WizardStep
  onStepClick?: (step: WizardStep) => void
  /** Steps habilitados para navegar hacia atrás */
  completedUpTo?: WizardStep
}

export function WizardStepper({ current, onStepClick, completedUpTo = 1 }: Props) {
  return (
    <nav aria-label="Progreso del wizard">
      <ol className="flex items-center">
        {STEPS.map((step, idx) => {
          const num     = (idx + 1) as WizardStep
          const done    = num < current
          const active  = num === current
          const clickable = num <= completedUpTo && onStepClick

          return (
            <li key={num} className="flex-1 flex items-center">
              {/* Step circle */}
              <button
                onClick={() => clickable && onStepClick(num)}
                disabled={!clickable}
                className={`flex items-center gap-2 group ${clickable ? 'cursor-pointer' : 'cursor-default'}`}
                aria-current={active ? 'step' : undefined}
              >
                <span
                  className={`
                    w-8 h-8 rounded-full flex items-center justify-center shrink-0
                    text-sm font-semibold border-2 transition-colors
                    ${done   ? 'bg-blue-600 border-blue-600 text-white' : ''}
                    ${active ? 'border-blue-600 bg-white text-blue-600' : ''}
                    ${!done && !active ? 'border-slate-300 bg-white text-slate-400' : ''}
                  `}
                >
                  {done ? <Check className="w-4 h-4" /> : num}
                </span>
                <span className="hidden sm:block text-left">
                  <span className={`text-xs font-semibold block ${active ? 'text-blue-600' : done ? 'text-slate-700' : 'text-slate-400'}`}>
                    {step.label}
                  </span>
                  <span className="text-xs text-slate-400">{step.description}</span>
                </span>
              </button>

              {/* Connector */}
              {idx < STEPS.length - 1 && (
                <div className={`flex-1 h-0.5 mx-3 transition-colors ${num < current ? 'bg-blue-600' : 'bg-slate-200'}`} />
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}
