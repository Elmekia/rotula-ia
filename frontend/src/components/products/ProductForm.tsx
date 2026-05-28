import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { X } from 'lucide-react'
import type { Product, ProductRequest } from '../../types/product'

const schema = z.object({
  name:               z.string().min(1, 'El nombre es obligatorio').max(300),
  category:           z.string().min(1, 'La categoría es obligatoria').max(100),
  netWeight:          z.coerce.number({ invalid_type_error: 'Debe ser un número' }).positive('Debe ser mayor a 0'),
  weightUnit:         z.enum(['kg', 'g', 'l', 'ml', 'u', 'cc'], { errorMap: () => ({ message: 'Unidad inválida' }) }),
  rneNumber:          z.string().max(20).nullable().optional(),
  rnpaNumber:         z.string().max(20).nullable().optional(),
  servingSizeG:       z.preprocess(
    (v) => (v === '' || v === null || v === undefined ? null : Number(v)),
    z.number().positive('Debe ser mayor a 0').nullable().optional()
  ),
  crossContamination: z.string().nullable().optional(),
})

type FormValues = z.infer<typeof schema>

interface Props {
  product?: Product | null
  isSubmitting: boolean
  onSubmit: (data: ProductRequest) => void
  onClose: () => void
}

export function ProductForm({ product, isSubmitting, onSubmit, onClose }: Props) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name:               '',
      category:           '',
      netWeight:          undefined,
      weightUnit:         'g',
      rneNumber:          '',
      rnpaNumber:         '',
      servingSizeG:       null,
      crossContamination: '',
    },
  })

  // Pre-fill when editing
  useEffect(() => {
    if (product) {
      reset({
        name:               product.name,
        category:           product.category,
        netWeight:          product.netWeight,
        weightUnit:         product.weightUnit as FormValues['weightUnit'],
        rneNumber:          product.rneNumber ?? '',
        rnpaNumber:         product.rnpaNumber ?? '',
        servingSizeG:       product.servingSizeG ?? null,
        crossContamination: product.crossContamination ?? '',
      })
    } else {
      reset({
        name:               '',
        category:           '',
        netWeight:          undefined,
        weightUnit:         'g',
        rneNumber:          '',
        rnpaNumber:         '',
        servingSizeG:       null,
        crossContamination: '',
      })
    }
  }, [product, reset])

  function onValid(values: FormValues) {
    onSubmit({
      name:               values.name,
      category:           values.category,
      netWeight:          values.netWeight,
      weightUnit:         values.weightUnit,
      rneNumber:          values.rneNumber || null,
      rnpaNumber:         values.rnpaNumber || null,
      servingSizeG:       values.servingSizeG ?? null,
      crossContamination: values.crossContamination || null,
    })
  }

  return (
    /* Backdrop */
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4 bg-black/30 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] flex flex-col">
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
          {/* Nombre */}
          <Field label="Nombre *" error={errors.name?.message}>
            <input
              {...register('name')}
              placeholder="Ej. Yerba Mate Suave"
              className={inputCls(!!errors.name)}
            />
          </Field>

          {/* Categoría */}
          <Field label="Categoría *" error={errors.category?.message}>
            <input
              {...register('category')}
              placeholder="Ej. Infusiones"
              className={inputCls(!!errors.category)}
            />
          </Field>

          {/* Peso neto + Unidad */}
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

          {/* Porción */}
          <Field
            label="Porción de referencia (g / mL)"
            hint="Necesario para calcular la tabla nutricional"
            error={errors.servingSizeG?.message}
          >
            <input
              {...register('servingSizeG')}
              type="number"
              step="any"
              min="0.001"
              placeholder="Ej. 30"
              className={inputCls(!!errors.servingSizeG)}
            />
          </Field>

          {/* RNE / RNPA */}
          <div className="grid grid-cols-2 gap-3">
            <Field label="Número RNE" error={errors.rneNumber?.message}>
              <input
                {...register('rneNumber')}
                placeholder="Opcional"
                className={inputCls(!!errors.rneNumber)}
              />
            </Field>
            <Field label="Número RNPA" error={errors.rnpaNumber?.message}>
              <input
                {...register('rnpaNumber')}
                placeholder="Opcional"
                className={inputCls(!!errors.rnpaNumber)}
              />
            </Field>
          </div>

          {/* Contaminación cruzada */}
          <Field
            label="Contaminación cruzada"
            hint="Nombres de enum separados por coma: MANI, FRUTOS_DE_CASCARA, etc."
            error={errors.crossContamination?.message}
          >
            <input
              {...register('crossContamination')}
              placeholder="Ej. MANI, FRUTOS_DE_CASCARA"
              className={inputCls(!!errors.crossContamination)}
            />
          </Field>

          {/* Footer */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800
                         border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600
                         hover:bg-blue-700 disabled:opacity-60 rounded-lg transition-colors
                         flex items-center gap-2"
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

// ── helpers ──────────────────────────────────────────────────────────────────

function inputCls(hasError: boolean) {
  return [
    'w-full px-3 py-2 text-sm border rounded-lg outline-none transition-colors',
    hasError
      ? 'border-red-400 focus:ring-2 focus:ring-red-300'
      : 'border-slate-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100',
  ].join(' ')
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
