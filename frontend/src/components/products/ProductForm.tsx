import { useEffect, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { X, ChevronDown, ChevronUp, AlertTriangle, Info } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { foodReferenceApi } from '../../lib/foodReferenceApi'
import type { Product, ProductRequest } from '../../types/product'

// ── Alérgenos Res. 109/2023 ───────────────────────────────────────────────────

const ALLERGEN_OPTIONS = [
  { value: 'GLUTEN',            label: 'Gluten (trigo, cebada, centeno, avena)' },
  { value: 'CRUSTACEOS',        label: 'Crustáceos' },
  { value: 'HUEVO',             label: 'Huevo' },
  { value: 'PESCADO',           label: 'Pescado' },
  { value: 'MANI',              label: 'Maní' },
  { value: 'SOJA',              label: 'Soja' },
  { value: 'LECHE',             label: 'Leche' },
  { value: 'FRUTOS_DE_CASCARA', label: 'Frutos de cáscara' },
  { value: 'APIO',              label: 'Apio' },
  { value: 'MOSTAZA',           label: 'Mostaza' },
  { value: 'SESAMO',            label: 'Sésamo' },
  { value: 'SULFITOS',          label: 'Sulfitos' },
  { value: 'ALTRAMUCES',        label: 'Altramuces' },
  { value: 'MOLUSCOS',          label: 'Moluscos' },
] as const

// ── Zod schema ────────────────────────────────────────────────────────────────

const optNonNeg = z.preprocess(
  (v) => (v === '' || v === null || v === undefined ? null : Number(v)),
  z.number().min(0, 'Debe ser 0 o mayor').nullable().optional()
)

const schema = z.object({
  name:         z.string().min(1, 'El nombre es obligatorio').max(300),
  denomination: z.string().min(1, 'La denominación es obligatoria').max(300),

  foodGroupId: z.string().uuid('Seleccioná un grupo de alimentos'),
  foodItemId:  z.string().uuid('Seleccioná un alimento'),

  netWeight:  z.coerce.number({ invalid_type_error: 'Debe ser un número' }).positive('Debe ser mayor a 0'),
  weightUnit: z.enum(['kg', 'g', 'l', 'ml', 'u', 'cc'], {
    errorMap: () => ({ message: 'Unidad inválida' }),
  }),

  rneNumber:  z.string().max(20).nullable().optional(),
  rnpaNumber: z.string().min(1, 'El RNPA es obligatorio').max(20),

  crossContaminationGroups: z.array(z.string()).default([]),
  showIngredientPercentages: z.boolean().default(false),

  energyKcalPer100g: optNonNeg,
  proteinsPer100g:   optNonNeg,
  carbsPer100g:      optNonNeg,
  sugarsPer100g:     optNonNeg,
  fatTotalPer100g:   optNonNeg,
  fatSatPer100g:     optNonNeg,
  fatTransPer100g:   optNonNeg,
  sodiumMgPer100g:   optNonNeg,
})

type FormValues = z.infer<typeof schema>

interface Props {
  product?:    Product | null
  isSubmitting: boolean
  onSubmit:    (data: ProductRequest) => void
  onClose:     () => void
}

// ── Component ─────────────────────────────────────────────────────────────────

export function ProductForm({ product, isSubmitting, onSubmit, onClose }: Props) {
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
      name:         '',
      denomination: '',
      foodGroupId:  '',
      foodItemId:   '',
      netWeight:    undefined,
      weightUnit:   'g',
      rneNumber:    '',
      rnpaNumber:   '',
      crossContaminationGroups:  [],
      showIngredientPercentages: false,
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

  // ── Datos de referencia ──────────────────────────────────────────────────

  const { data: foodGroups = [] } = useQuery({
    queryKey: ['food-groups'],
    queryFn:  foodReferenceApi.listGroups,
    staleTime: Infinity,
  })

  const selectedGroupId = watch('foodGroupId')
  const selectedItemId  = watch('foodItemId')

  const { data: foodItems = [] } = useQuery({
    queryKey: ['food-items', selectedGroupId],
    queryFn:  () => foodReferenceApi.listItems(selectedGroupId),
    enabled:  !!selectedGroupId,
    staleTime: Infinity,
  })

  // Porción auto-completada desde el alimento seleccionado
  const selectedItem = foodItems.find((i) => i.id === selectedItemId)

  // Al cambiar grupo, limpiar alimento seleccionado
  const prevGroupId = watch('foodGroupId')
  useEffect(() => {
    setValue('foodItemId', '')
  }, [prevGroupId, setValue])

  // ── Pre-fill al editar ───────────────────────────────────────────────────

  useEffect(() => {
    if (product) {
      reset({
        name:         product.name,
        denomination: product.denomination ?? '',
        foodGroupId:  product.foodGroupId ?? '',
        foodItemId:   product.foodItemId  ?? '',
        netWeight:    product.netWeight,
        weightUnit:   product.weightUnit as FormValues['weightUnit'],
        rneNumber:    product.rneNumber  ?? '',
        rnpaNumber:   product.rnpaNumber ?? '',
        crossContaminationGroups:  product.crossContaminationGroups ?? [],
        showIngredientPercentages: product.showIngredientPercentages ?? false,
        energyKcalPer100g: product.energyKcalPer100g ?? null,
        proteinsPer100g:   product.proteinsPer100g   ?? null,
        carbsPer100g:      product.carbsPer100g       ?? null,
        sugarsPer100g:     product.sugarsPer100g      ?? null,
        fatTotalPer100g:   product.fatTotalPer100g    ?? null,
        fatSatPer100g:     product.fatSatPer100g      ?? null,
        fatTransPer100g:   product.fatTransPer100g    ?? null,
        sodiumMgPer100g:   product.sodiumMgPer100g    ?? null,
      })
      // Abrir sección nutricional si ya tiene datos
      const hasNutrition = [
        product.energyKcalPer100g, product.proteinsPer100g,
        product.carbsPer100g,      product.sugarsPer100g,
        product.fatTotalPer100g,   product.fatSatPer100g,
        product.fatTransPer100g,   product.sodiumMgPer100g,
      ].some((v) => v != null)
      if (hasNutrition) setShowNutrition(true)
    } else {
      reset({
        name:         '',
        denomination: '',
        foodGroupId:  '',
        foodItemId:   '',
        netWeight:    undefined,
        weightUnit:   'g',
        rneNumber:    '',
        rnpaNumber:   '',
        crossContaminationGroups:  [],
        showIngredientPercentages: false,
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
  }, [product, reset])

  // ── Submit ───────────────────────────────────────────────────────────────

  function onValid(values: FormValues) {
    onSubmit({
      name:         values.name,
      denomination: values.denomination,
      foodGroupId:  values.foodGroupId,
      foodItemId:   values.foodItemId,
      servingSizeG: selectedItem?.portionGrams ?? null,
      netWeight:    values.netWeight,
      weightUnit:   values.weightUnit,
      rneNumber:    values.rneNumber || null,
      rnpaNumber:   values.rnpaNumber,
      crossContaminationGroups:  values.crossContaminationGroups,
      showIngredientPercentages: values.showIngredientPercentages,
      energyKcalPer100g: values.energyKcalPer100g ?? null,
      proteinsPer100g:   values.proteinsPer100g   ?? null,
      carbsPer100g:      values.carbsPer100g       ?? null,
      sugarsPer100g:     values.sugarsPer100g      ?? null,
      fatTotalPer100g:   values.fatTotalPer100g    ?? null,
      fatSatPer100g:     values.fatSatPer100g      ?? null,
      fatTransPer100g:   values.fatTransPer100g    ?? null,
      sodiumMgPer100g:   values.sodiumMgPer100g    ?? null,
    })
  }

  const crossContaminationGroups = watch('crossContaminationGroups')

  // ── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-xl max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 shrink-0">
          <h2 className="text-lg font-semibold text-slate-800">
            {product ? 'Editar producto' : 'Nuevo producto'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit(onValid)} className="overflow-y-auto px-6 py-5 space-y-4">

          {/* ── Identificación ────────────────────────────────────────────── */}
          <SectionTitle>Identificación</SectionTitle>

          <Field label="Nombre interno *" hint="Solo para uso interno, no aparece en el rótulo" error={errors.name?.message}>
            <input {...register('name')} placeholder="Ej. Galletitas de avena lote A" className={inputCls(!!errors.name)} />
          </Field>

          <Field label="Denominación del alimento *" hint="Aparece en el rótulo según el CAA" error={errors.denomination?.message}>
            <input {...register('denomination')} placeholder="Ej. Galletitas dulces de avena con chips de chocolate" className={inputCls(!!errors.denomination)} />
          </Field>

          {/* ── Clasificación TABLA I ──────────────────────────────────────── */}
          <SectionTitle>Clasificación (TABLA I)</SectionTitle>

          <Field label="Grupo de alimentos *" error={errors.foodGroupId?.message}>
            <select {...register('foodGroupId')} className={inputCls(!!errors.foodGroupId)}>
              <option value="">— Seleccioná un grupo —</option>
              {foodGroups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </Field>

          <Field label="Alimento de referencia *" error={errors.foodItemId?.message}>
            <select
              {...register('foodItemId')}
              disabled={!selectedGroupId}
              className={inputCls(!!errors.foodItemId)}
            >
              <option value="">
                {selectedGroupId ? '— Seleccioná un alimento —' : '— Primero seleccioná el grupo —'}
              </option>
              {foodItems.map((i) => (
                <option key={i.id} value={i.id}>{i.name}</option>
              ))}
            </select>
          </Field>

          {/* Porción: read-only, auto-completada */}
          {selectedItem && (
            <div className="flex items-center gap-2 px-3 py-2 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
              <Info className="w-4 h-4 shrink-0" />
              <span>
                Porción de referencia según TABLA I:&nbsp;
                <strong>{selectedItem.portionGrams} {selectedItem.unit}</strong>
              </span>
            </div>
          )}

          {/* ── Presentación ───────────────────────────────────────────────── */}
          <SectionTitle>Presentación</SectionTitle>

          <div className="grid grid-cols-2 gap-3">
            <Field label="Peso neto *" error={errors.netWeight?.message}>
              <input
                {...register('netWeight')}
                type="number"
                step="any"
                placeholder="500"
                className={inputCls(!!errors.netWeight)}
              />
            </Field>
            <Field label="Unidad *" error={errors.weightUnit?.message}>
              <select {...register('weightUnit')} className={inputCls(!!errors.weightUnit)}>
                <option value="g">g</option>
                <option value="kg">kg</option>
                <option value="ml">ml</option>
                <option value="l">l</option>
                <option value="cc">cc</option>
                <option value="u">u (unidades)</option>
              </select>
            </Field>
          </div>

          {/* ── Registros ─────────────────────────────────────────────────── */}
          <SectionTitle>Registros</SectionTitle>

          <div className="grid grid-cols-2 gap-3">
            <Field label="RNPA *" error={errors.rnpaNumber?.message}>
              <input
                {...register('rnpaNumber')}
                placeholder="Ej. 01234567"
                className={inputCls(!!errors.rnpaNumber)}
              />
            </Field>
            <Field label="RNE" error={errors.rneNumber?.message}>
              <input
                {...register('rneNumber')}
                placeholder="Opcional"
                className={inputCls(!!errors.rneNumber)}
              />
            </Field>
          </div>

          {/* ── Contaminación cruzada ─────────────────────────────────────── */}
          <SectionTitle>Contaminación cruzada</SectionTitle>
          <p className="text-xs text-slate-400 -mt-2">
            Marcá los alérgenos presentes en el ambiente de producción (Res. 109/2023).
          </p>

          <Controller
            name="crossContaminationGroups"
            control={control}
            render={({ field }) => (
              <div className="grid grid-cols-2 gap-1.5">
                {ALLERGEN_OPTIONS.map(({ value, label }) => {
                  const checked = field.value.includes(value)
                  return (
                    <label
                      key={value}
                      className={`flex items-center gap-2 px-3 py-2 rounded-lg border text-sm cursor-pointer transition-colors ${
                        checked
                          ? 'bg-amber-50 border-amber-300 text-amber-800'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <input
                        type="checkbox"
                        className="accent-amber-500 shrink-0"
                        checked={checked}
                        onChange={(e) => {
                          const next = e.target.checked
                            ? [...field.value, value]
                            : field.value.filter((v) => v !== value)
                          field.onChange(next)
                        }}
                      />
                      {label}
                    </label>
                  )
                })}
              </div>
            )}
          />

          {crossContaminationGroups.length > 0 && (
            <div className="flex items-start gap-1.5 text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded px-2 py-1.5">
              <AlertTriangle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
              <span>
                Se declarará contaminación cruzada con:{' '}
                <strong>
                  {crossContaminationGroups
                    .map((v) => ALLERGEN_OPTIONS.find((o) => o.value === v)?.label ?? v)
                    .join(', ')}
                </strong>
              </span>
            </div>
          )}

          {/* ── Opciones de rótulo ─────────────────────────────────────────── */}
          <SectionTitle>Opciones de rótulo</SectionTitle>

          <Controller
            name="showIngredientPercentages"
            control={control}
            render={({ field }) => (
              <div className="flex items-center justify-between py-2 px-3 bg-slate-50 rounded-lg">
                <div>
                  <p className="text-sm font-medium text-slate-700">Mostrar % de ingredientes</p>
                  <p className="text-xs text-slate-500">Optativo · se muestra en la lista de ingredientes del rótulo</p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={field.value}
                  onClick={() => field.onChange(!field.value)}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    field.value ? 'bg-blue-500' : 'bg-slate-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                      field.value ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>
            )}
          />

          {/* ── Tabla nutricional ─────────────────────────────────────────── */}
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
                  Ingresá los valores <strong>por cada 100 g</strong> del producto final.
                  {selectedItem && (
                    <> El sistema calculará automáticamente los valores por porción ({selectedItem.portionGrams} {selectedItem.unit}).</>
                  )}
                </p>

                <div className="grid grid-cols-2 gap-3">
                  <NutrientField label="Energía (kcal)"       placeholder="0" error={errors.energyKcalPer100g} {...register('energyKcalPer100g')} />
                  <NutrientField label="Proteínas (g)"        placeholder="0" error={errors.proteinsPer100g}   {...register('proteinsPer100g')} />
                  <NutrientField label="Carbohidratos (g)"    placeholder="0" error={errors.carbsPer100g}      {...register('carbsPer100g')} />
                  <NutrientField label="Azúcares (g)"         placeholder="0" error={errors.sugarsPer100g}     {...register('sugarsPer100g')} />
                  <NutrientField label="Grasas totales (g)"   placeholder="0" error={errors.fatTotalPer100g}   {...register('fatTotalPer100g')} />
                  <NutrientField label="Grasas saturadas (g)" placeholder="0" error={errors.fatSatPer100g}     {...register('fatSatPer100g')} />
                  <NutrientField label="Grasas trans (g)"     placeholder="0" error={errors.fatTransPer100g}   {...register('fatTransPer100g')} />
                  <NutrientField label="Sodio (mg)"           placeholder="0" error={errors.sodiumMgPer100g}   {...register('sodiumMgPer100g')} />
                </div>
              </div>
            )}
          </div>

          {/* ── Footer ──────────────────────────────────────────────────────── */}
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
              {product ? 'Guardar cambios' : 'Crear producto'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function inputCls(hasError: boolean) {
  return [
    'w-full px-3 py-2 text-sm border rounded-lg outline-none transition-colors',
    hasError
      ? 'border-red-400 focus:ring-2 focus:ring-red-300'
      : 'border-slate-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100',
  ].join(' ')
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400 pt-2 border-t border-slate-100">
      {children}
    </h3>
  )
}

function Field({
  label, hint, error, children,
}: {
  label: string
  hint?: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1">
      <label className="block text-xs font-medium text-slate-600">{label}</label>
      {hint && <p className="text-xs text-slate-400">{hint}</p>}
      {children}
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  )
}

import { forwardRef } from 'react'
import type { FieldError } from 'react-hook-form'

const NutrientField = forwardRef<
  HTMLInputElement,
  { label: string; placeholder: string; error?: FieldError; [k: string]: unknown }
>(({ label, placeholder, error, ...props }, ref) => (
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
))
