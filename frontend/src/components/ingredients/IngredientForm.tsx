import { forwardRef, useEffect, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { X, AlertTriangle, ChevronDown, ChevronUp } from 'lucide-react'
import { detectAllergen } from '../../lib/allergenDetector'
import type { Ingredient, IngredientRequest } from '../../types/ingredient'
import type { FieldError } from 'react-hook-form'

const optionalNonNegativeNumber = z.preprocess(
  (v) => (v === '' || v === null || v === undefined ? null : Number(v)),
  z.number().min(0, 'Debe ser 0 o mayor').nullable().optional()
)

const schema = z.object({
  name: z.string().min(1, 'El nombre es obligatorio').max(300),
  weightGrams: z.coerce
    .number({ invalid_type_error: 'Debe ser un número' })
    .gt(0, 'Debe ser mayor a 0'),
  allergen: z.boolean(),
  // Campos nutricionales opcionales (0 es válido, ej. grasas trans = 0 g)
  energyKcalPer100g: optionalNonNegativeNumber,
  proteinsPer100g:   optionalNonNegativeNumber,
  carbsPer100g:      optionalNonNegativeNumber,
  sugarsPer100g:     optionalNonNegativeNumber,
  fatTotalPer100g:   optionalNonNegativeNumber,
  fatSatPer100g:     optionalNonNegativeNumber,
  fatTransPer100g:   optionalNonNegativeNumber,
  sodiumMgPer100g:   optionalNonNegativeNumber,
})

type FormValues = z.infer<typeof schema>

interface Props {
  ingredient?: Ingredient | null
  isSubmitting: boolean
  onSubmit: (data: IngredientRequest) => void
  onClose: () => void
}

export function IngredientForm({ ingredient, isSubmitting, onSubmit, onClose }: Props) {
  const [showNutrition, setShowNutrition] = useState(false)

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
      name:              '',
      weightGrams:       undefined,
      allergen:          false,
      energyKcalPer100g: null,
      proteinsPer100g:   null,
      carbsPer100g:      null,
      sugarsPer100g:     null,
      fatTotalPer100g:   null,
      fatSatPer100g:     null,
      fatTransPer100g:   null,
      sodiumMgPer100g:   null,
    },
  })

  // Pre-fill cuando se edita
  useEffect(() => {
    if (ingredient) {
      reset({
        name:              ingredient.name,
        weightGrams:       ingredient.weightGrams,
        allergen:          ingredient.allergen,
        energyKcalPer100g: ingredient.energyKcalPer100g ?? null,
        proteinsPer100g:   ingredient.proteinsPer100g ?? null,
        carbsPer100g:      ingredient.carbsPer100g ?? null,
        sugarsPer100g:     ingredient.sugarsPer100g ?? null,
        fatTotalPer100g:   ingredient.fatTotalPer100g ?? null,
        fatSatPer100g:     ingredient.fatSatPer100g ?? null,
        fatTransPer100g:   ingredient.fatTransPer100g ?? null,
        sodiumMgPer100g:   ingredient.sodiumMgPer100g ?? null,
      })
      // Abrir sección si ya tiene algún dato nutricional cargado
      const hasNutrition = [
        ingredient.energyKcalPer100g,
        ingredient.proteinsPer100g,
        ingredient.carbsPer100g,
        ingredient.sugarsPer100g,
        ingredient.fatTotalPer100g,
        ingredient.fatSatPer100g,
        ingredient.fatTransPer100g,
        ingredient.sodiumMgPer100g,
      ].some(v => v != null)
      if (hasNutrition) setShowNutrition(true)
    } else {
      reset({
        name:              '',
        weightGrams:       undefined,
        allergen:          false,
        energyKcalPer100g: null,
        proteinsPer100g:   null,
        carbsPer100g:      null,
        sugarsPer100g:     null,
        fatTotalPer100g:   null,
        fatSatPer100g:     null,
        fatTransPer100g:   null,
        sodiumMgPer100g:   null,
      })
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
      name:              values.name,
      weightGrams:       values.weightGrams,
      allergen:          values.allergen,
      energyKcalPer100g: values.energyKcalPer100g ?? null,
      proteinsPer100g:   values.proteinsPer100g ?? null,
      carbsPer100g:      values.carbsPer100g ?? null,
      sugarsPer100g:     values.sugarsPer100g ?? null,
      fatTotalPer100g:   values.fatTotalPer100g ?? null,
      fatSatPer100g:     values.fatSatPer100g ?? null,
      fatTransPer100g:   values.fatTransPer100g ?? null,
      sodiumMgPer100g:   values.sodiumMgPer100g ?? null,
    })
  }

  const isAllergenDetected = detectAllergen(nameValue)

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 shrink-0">
          <h2 className="text-lg font-semibold text-slate-800">
            {ingredient ? 'Editar ingrediente' : 'Agregar ingrediente'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onValid)} className="overflow-y-auto px-6 py-5 space-y-4">
          {/* Nombre */}
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-600">Nombre *</label>
            <input
              {...register('name')}
              placeholder="Ej. Harina de trigo"
              className={inputCls(!!errors.name)}
            />
            {errors.name && <p className="text-xs text-red-500">{errors.name.message}</p>}

            {/* Alerta de alérgeno detectado */}
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

          {/* ── Sección nutricional (colapsable) ─────────────────────────────── */}
          <div className="border border-slate-200 rounded-lg overflow-hidden">
            <button
              type="button"
              onClick={() => setShowNutrition(!showNutrition)}
              className="w-full flex items-center justify-between px-4 py-3 bg-slate-50
                         hover:bg-slate-100 transition-colors text-left"
            >
              <span className="text-xs font-medium text-slate-600">
                Información nutricional por 100 g
                <span className="ml-1.5 font-normal text-slate-400">(opcional)</span>
              </span>
              {showNutrition
                ? <ChevronUp className="w-4 h-4 text-slate-400" />
                : <ChevronDown className="w-4 h-4 text-slate-400" />}
            </button>

            {showNutrition && (
              <div className="px-4 py-4 space-y-3">
                <p className="text-xs text-slate-400">
                  Completá los datos nutricionales del ingrediente <em>por cada 100 g</em>. Se usarán para
                  calcular la tabla nutricional del producto.
                </p>

                <div className="grid grid-cols-2 gap-3">
                  <NutrientField label="Energía (kcal)"      placeholder="0" error={errors.energyKcalPer100g} {...register('energyKcalPer100g')} />
                  <NutrientField label="Proteínas (g)"       placeholder="0" error={errors.proteinsPer100g}   {...register('proteinsPer100g')} />
                  <NutrientField label="Carbohidratos (g)"   placeholder="0" error={errors.carbsPer100g}      {...register('carbsPer100g')} />
                  <NutrientField label="Azúcares (g)"        placeholder="0" error={errors.sugarsPer100g}     {...register('sugarsPer100g')} />
                  <NutrientField label="Grasas totales (g)"  placeholder="0" error={errors.fatTotalPer100g}   {...register('fatTotalPer100g')} />
                  <NutrientField label="Grasas saturadas (g)" placeholder="0" error={errors.fatSatPer100g}   {...register('fatSatPer100g')} />
                  <NutrientField label="Grasas trans (g)"    placeholder="0" error={errors.fatTransPer100g}   {...register('fatTransPer100g')} />
                  <NutrientField label="Sodio (mg)"          placeholder="0" error={errors.sodiumMgPer100g}   {...register('sodiumMgPer100g')} />
                </div>
              </div>
            )}
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

/**
 * forwardRef is required so that RHF's ref callback (from register()) reaches
 * the <input> DOM element. Without it, React 18 does NOT pass the ref through
 * a plain function component, so RHF never sets _f.mount = true and the
 * onChange handler becomes a no-op — values stay null on submit.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const NutrientField = forwardRef<HTMLInputElement, { label: string; placeholder: string; error?: FieldError; [k: string]: any }>(
  ({ label, placeholder, error, ...props }, ref) => (
    <div className="space-y-0.5">
      <label className="block text-xs text-slate-500">{label}</label>
      <input
        ref={ref}
        type="number"
        step="0.01"
        min="0"
        placeholder={placeholder}
        className={[
          'w-full px-2.5 py-1.5 text-sm border rounded-lg outline-none transition-colors',
          error
            ? 'border-red-400 focus:ring-2 focus:ring-red-300'
            : 'border-slate-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100',
        ].join(' ')}
        {...props}
      />
      {error && <p className="text-xs text-red-500">{error.message}</p>}
    </div>
  )
)
