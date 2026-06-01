import { useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { X, AlertTriangle } from 'lucide-react'
import { detectAllergen } from '../../lib/allergenDetector'
import type { Ingredient, IngredientRequest } from '../../types/ingredient'

const schema = z.object({
  name: z.string().min(1, 'El nombre es obligatorio').max(300),
  weightGrams: z.coerce
    .number({ invalid_type_error: 'Debe ser un número' })
    .gt(0, 'Debe ser mayor a 0'),
  allergen: z.boolean(),
})

type FormValues = z.infer<typeof schema>

interface Props {
  ingredient?:  Ingredient | null
  isSubmitting: boolean
  onSubmit:     (data: IngredientRequest) => void
  onClose:      () => void
}

export function IngredientForm({ ingredient, isSubmitting, onSubmit, onClose }: Props) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name:        '',
      weightGrams: undefined,
      allergen:    false,
    },
  })

  // Pre-fill al editar
  useEffect(() => {
    if (ingredient) {
      reset({
        name:        ingredient.name,
        weightGrams: ingredient.weightGrams,
        allergen:    ingredient.allergen,
      })
    } else {
      reset({ name: '', weightGrams: undefined, allergen: false })
    }
  }, [ingredient, reset])

  // Detección de alérgeno en tiempo real mientras el usuario escribe
  const nameValue = watch('name')
  useEffect(() => {
    if (!ingredient) {
      setValue('allergen', detectAllergen(nameValue))
    }
  }, [nameValue, ingredient, setValue])

  function onValid(values: FormValues) {
    onSubmit({
      name:        values.name,
      weightGrams: values.weightGrams,
      allergen:    values.allergen,
    })
  }

  const isAllergenDetected = detectAllergen(nameValue)

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 shrink-0">
          <h2 className="text-lg font-semibold text-slate-800">
            {ingredient ? 'Editar ingrediente' : 'Agregar ingrediente'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onValid)} className="px-6 py-5 space-y-4">
          {/* Nombre */}
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-600">Nombre *</label>
            <input
              {...register('name')}
              placeholder="Ej. Harina de trigo"
              className={inputCls(!!errors.name)}
            />
            {errors.name && <p className="text-xs text-red-500">{errors.name.message}</p>}

            {isAllergenDetected && (
              <div className="flex items-center gap-1.5 text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded px-2 py-1.5">
                <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                <span>Alérgeno detectado según Res. 109/2023</span>
              </div>
            )}
          </div>

          {/* Peso en gramos */}
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-600">
              Peso (g) *
              <span className="ml-1 font-normal text-slate-400">— el % se calcula automáticamente</span>
            </label>
            <input
              {...register('weightGrams')}
              type="number"
              step="0.001"
              min="0.001"
              placeholder="200.000"
              className={inputCls(!!errors.weightGrams)}
            />
            {errors.weightGrams && <p className="text-xs text-red-500">{errors.weightGrams.message}</p>}
          </div>

          {/* Alérgeno (toggle) */}
          <div className="flex items-center justify-between py-2 px-3 bg-slate-50 rounded-lg">
            <div>
              <p className="text-sm font-medium text-slate-700">¿Es alérgeno?</p>
              <p className="text-xs text-slate-500">Según Res. 109/2023 · podés modificarlo manualmente</p>
            </div>
            <Controller
              name="allergen"
              control={control}
              render={({ field }) => (
                <button
                  type="button"
                  role="switch"
                  aria-checked={field.value}
                  onClick={() => field.onChange(!field.value)}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    field.value ? 'bg-amber-500' : 'bg-slate-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                      field.value ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              )}
            />
          </div>

          {/* Footer */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 border border-slate-200
                         rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700
                         disabled:opacity-60 rounded-lg transition-colors flex items-center gap-2"
            >
              {isSubmitting && (
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              )}
              {ingredient ? 'Guardar' : 'Agregar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── helpers ───────────────────────────────────────────────────────────────────

function inputCls(hasError: boolean) {
  return [
    'w-full px-3 py-2 text-sm border rounded-lg outline-none transition-colors',
    hasError
      ? 'border-red-400 focus:ring-2 focus:ring-red-300'
      : 'border-slate-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100',
  ].join(' ')
}
