import { useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Sparkles, AlertTriangle, CheckCircle2, Info, Loader2 } from 'lucide-react'
import { productsApi } from '../lib/productsApi'
import { ingredientsApi } from '../lib/ingredientsApi'
import { analysisApi } from '../lib/analysisApi'
import { labelsApi } from '../lib/labelsApi'
import { legalNameApi } from '../lib/legalNameApi'
import { useWizardStore } from '../store/wizardStore'
import { useToastStore } from '../store/toastStore'
import { WizardStepper } from '../components/wizard/WizardStepper'
import { LabelPreview } from '../components/preview/LabelPreview'
import { IngredientList } from '../components/ingredients/IngredientList'
import type { LabelDTO } from '../types/label'

export function LabelWizard() {
  const navigate = useNavigate()
  const qc       = useQueryClient()
  const { addToast } = useToastStore()

  const { step, productId, setStep, nextStep, prevStep, setProductId, reset } = useWizardStore()

  // Limpiar wizard al desmontar
  useEffect(() => () => { reset() }, [reset])

  // ── Datos ────────────────────────────────────────────────────────────────
  const { data: products = [], isLoading: productsLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => productsApi.list(0, 100).then(p => p.content),
  })

  const selectedProduct = products.find(p => p.id === productId) ?? null

  const { data: ingredients = [] } = useQuery({
    queryKey: ['ingredients', productId],
    queryFn: () => ingredientsApi.list(productId!),
    enabled: !!productId,
  })

  const { data: analysis, isLoading: analysisLoading } = useQuery({
    queryKey: ['analysis', productId],
    queryFn: () => analysisApi.analyze(productId!),
    enabled: !!productId && step >= 3,
    staleTime: 15_000,
  })

  const { data: legalName } = useQuery({
    queryKey: ['legal-name', productId],
    queryFn: () => legalNameApi.getSuggestion(productId!),
    enabled: !!productId && step >= 4,
  })

  // ── Generación ────────────────────────────────────────────────────────────
  const generateMut = useMutation({
    mutationFn: () => labelsApi.generate(productId!),
    onSuccess(data) {
      qc.invalidateQueries({ queryKey: ['label-versions', productId] })
      addToast(`Rótulo v${data.version} generado correctamente`)
      navigate('/labels')
    },
    onError() {
      addToast('No se pudo generar el rótulo', 'error')
    },
  })

  // ── Validaciones por paso ─────────────────────────────────────────────────
  const canAdvanceStep1 = !!productId
  const canAdvanceStep2 = ingredients.length > 0
  const canAdvanceStep3 = true // nutrición es opcional

  // ── Paso 1: seleccionar producto ──────────────────────────────────────────
  function Step1() {
    return (
      <div className="space-y-4">
        <StepHeader
          title="Seleccioná un producto"
          description="El wizard generará el rótulo completo para el producto que elijas."
        />

        {productsLoading ? (
          <div className="flex items-center gap-2 text-slate-400 py-6">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Cargando productos…</span>
          </div>
        ) : products.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-slate-400">
            <p className="text-sm font-medium text-slate-600">No tenés productos creados</p>
            <Link to="/products" className="text-xs text-blue-500 hover:underline mt-1 inline-block">
              Creá un producto primero →
            </Link>
          </div>
        ) : (
          <div className="grid gap-2 max-h-96 overflow-y-auto pr-1">
            {products.map(p => (
              <button
                key={p.id}
                onClick={() => setProductId(p.id)}
                className={`
                  w-full text-left px-4 py-3 rounded-xl border transition-all
                  ${productId === p.id
                    ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                  }
                `}
              >
                <p className="text-sm font-semibold text-slate-800">{p.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">
                  {p.category} · {p.netWeight} {p.weightUnit}
                  {p.servingSizeG ? ` · Porción: ${p.servingSizeG} g` : ' · Sin porción configurada'}
                </p>
              </button>
            ))}
          </div>
        )}

        {productId && !selectedProduct?.servingSizeG && (
          <div className="flex items-start gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700">
            <AlertTriangle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
            <span>
              Este producto no tiene porción de referencia configurada. La tabla nutricional no podrá calcularse.
              <Link to={`/products/${productId}`} className="ml-1 font-medium underline">Editarlo →</Link>
            </span>
          </div>
        )}
      </div>
    )
  }

  // ── Paso 2: ingredientes ──────────────────────────────────────────────────
  function Step2() {
    return (
      <div className="space-y-4">
        <StepHeader
          title="Revisá los ingredientes"
          description="Los ingredientes se ordenan automáticamente de mayor a menor porcentaje."
        />
        {selectedProduct ? (
          <IngredientList product={selectedProduct} />
        ) : (
          <p className="text-sm text-slate-400">Seleccioná un producto primero.</p>
        )}
        {ingredients.length === 0 && (
          <div className="flex items-start gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700">
            <AlertTriangle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
            <span>Necesitás al menos un ingrediente para avanzar.</span>
          </div>
        )}
      </div>
    )
  }

  // ── Paso 3: nutrición ─────────────────────────────────────────────────────
  function Step3() {
    const hasAnyNutrition = (i: typeof ingredients[0]) =>
      i.energyKcalPer100g != null || i.proteinsPer100g  != null ||
      i.carbsPer100g       != null || i.sugarsPer100g    != null ||
      i.fatTotalPer100g    != null || i.fatSatPer100g    != null ||
      i.fatTransPer100g    != null || i.sodiumMgPer100g  != null

    const withNutrition    = ingredients.filter(i =>  hasAnyNutrition(i))
    const withoutNutrition = ingredients.filter(i => !hasAnyNutrition(i))
    const complete = withNutrition.length === ingredients.length && ingredients.length > 0

    return (
      <div className="space-y-4">
        <StepHeader
          title="Información nutricional"
          description="Revisá qué ingredientes tienen datos nutricionales cargados."
        />

        {/* Estado de completitud */}
        <div className={`flex items-center gap-3 p-4 rounded-xl border ${complete ? 'bg-emerald-50 border-emerald-200' : 'bg-slate-50 border-slate-200'}`}>
          {complete
            ? <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            : <Info className="w-5 h-5 text-slate-400 shrink-0" />}
          <div>
            <p className={`text-sm font-medium ${complete ? 'text-emerald-700' : 'text-slate-700'}`}>
              {complete
                ? 'Todos los ingredientes tienen datos nutricionales'
                : `${withNutrition.length} de ${ingredients.length} ingredientes con datos`}
            </p>
            {!complete && (
              <p className="text-xs text-slate-500 mt-0.5">
                La tabla nutricional se calculará usando solo los ingredientes con datos. Los datos nutricionales son opcionales.
              </p>
            )}
          </div>
        </div>

        {/* Ingredientes sin datos */}
        {withoutNutrition.length > 0 && (
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            <div className="px-4 py-2.5 border-b border-slate-100 bg-slate-50">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                Sin datos nutricionales
              </p>
            </div>
            {withoutNutrition.map(i => (
              <div key={i.id} className="flex items-center justify-between px-4 py-2.5 border-b border-slate-100 last:border-0">
                <span className="text-sm text-slate-700">{i.name}</span>
                <Link
                  to={`/products/${productId}`}
                  className="text-xs text-blue-500 hover:underline"
                >
                  Editar
                </Link>
              </div>
            ))}
          </div>
        )}

        {/* Preview de la tabla calculada */}
        {analysisLoading ? (
          <div className="flex items-center gap-2 text-slate-400 text-sm">
            <Loader2 className="w-4 h-4 animate-spin" />
            Calculando tabla nutricional…
          </div>
        ) : analysis?.nutrition ? (
          <div className="bg-white rounded-xl border border-slate-200 p-4">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
              Preview tabla nutricional
            </p>
            <NutritionPreview nutrition={analysis.nutrition} />
          </div>
        ) : null}
      </div>
    )
  }

  // ── Paso 4: revisión ──────────────────────────────────────────────────────
  function Step4() {
    // Construir un LabelDTO parcial para el preview en tiempo real
    const previewLabel: LabelDTO | null = selectedProduct && analysis
      ? {
          productId: selectedProduct.id,
          productName: selectedProduct.name,
          legalDenomination: legalName?.suggestedName ?? null,
          category: selectedProduct.category,
          netWeight: Number(selectedProduct.netWeight),
          weightUnit: selectedProduct.weightUnit,
          rneNumber: selectedProduct.rneNumber,
          rnpaNumber: selectedProduct.rnpaNumber,
          ingredients: ingredients.map(i => ({
            name: i.name,
            weightGrams: i.weightGrams,
            percentage: i.percentage,
            allergen: i.allergen,
          })),
          nutrition: analysis.nutrition,
          seals: [...(analysis.seals ?? [])],
          allergens: analysis.allergens,
          claims: legalName?.modifiers ?? [],
          legalNameAlert: legalName?.alertDiffers ?? false,
        }
      : null

    return (
      <div className="space-y-6">
        <StepHeader
          title="Revisión del rótulo"
          description="Verificá el contenido antes de confirmar la generación."
        />

        {/* Alerta de denominación legal */}
        {legalName?.alertDiffers && legalName.suggestedName && (
          <div className="flex items-start gap-2 p-3 bg-amber-50 border border-amber-300 rounded-lg text-sm text-amber-800">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <div>
              <strong>Denominación legal diferente:</strong>
              <span className="ml-1">El CAA sugiere llamarlo <em>"{legalName.suggestedName}"</em>.</span>
              {legalName.sourceArticle && (
                <span className="ml-1 text-amber-600">({legalName.sourceArticle})</span>
              )}
            </div>
          </div>
        )}

        {/* Claims */}
        {(legalName?.modifiers?.length ?? 0) > 0 && (
          <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3">
            <p className="text-xs font-semibold text-emerald-700 mb-1">Claims nutricionales detectados</p>
            <div className="flex flex-wrap gap-1.5">
              {legalName!.modifiers.map(m => (
                <span key={m} className="px-2 py-1 text-xs bg-emerald-100 text-emerald-700 rounded-full font-medium">
                  {m}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Preview del rótulo */}
        {previewLabel ? (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
              Preview del rótulo
            </p>
            <LabelPreview label={previewLabel} />
          </div>
        ) : (
          <div className="flex items-center gap-2 text-slate-400 text-sm py-4">
            <Loader2 className="w-4 h-4 animate-spin" />
            Cargando datos del rótulo…
          </div>
        )}
      </div>
    )
  }

  // ── Navegación ────────────────────────────────────────────────────────────
  const canAdvance = step === 1 ? canAdvanceStep1
    : step === 2 ? canAdvanceStep2
    : step === 3 ? canAdvanceStep3
    : false

  const isLastStep = step === 4

  // ── Render principal ──────────────────────────────────────────────────────
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Breadcrumb */}
      <Link
        to="/labels"
        className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 transition-colors"
      >
        <ChevronLeft className="w-4 h-4" />
        Rótulos
      </Link>

      {/* Stepper */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <WizardStepper
          current={step}
          completedUpTo={step}
          onStepClick={n => n < step && setStep(n)}
        />
      </div>

      {/* Step content */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        {step === 1 && <Step1 />}
        {step === 2 && <Step2 />}
        {step === 3 && <Step3 />}
        {step === 4 && <Step4 />}
      </div>

      {/* Navigation buttons */}
      <div className="flex items-center justify-between">
        <button
          onClick={prevStep}
          disabled={step === 1}
          className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-slate-600
                     border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors
                     disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronLeft className="w-4 h-4" />
          Anterior
        </button>

        {isLastStep ? (
          <button
            onClick={() => generateMut.mutate()}
            disabled={generateMut.isPending || !productId}
            className="flex items-center gap-2 px-5 py-2 text-sm font-medium text-white
                       bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors
                       disabled:opacity-60"
          >
            {generateMut.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Sparkles className="w-4 h-4" />
            )}
            Generar rótulo
          </button>
        ) : (
          <button
            onClick={nextStep}
            disabled={!canAdvance}
            className="flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-white
                       bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Siguiente
            <ChevronRight className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function StepHeader({ title, description }: { title: string; description: string }) {
  return (
    <div className="mb-4">
      <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
      <p className="text-sm text-slate-500 mt-0.5">{description}</p>
    </div>
  )
}

function NutritionPreview({ nutrition }: { nutrition: NonNullable<ReturnType<typeof analysisApi.analyze> extends Promise<infer T> ? T : never>['nutrition'] }) {
  if (!nutrition) return null
  const { per100g, perPortion, servingSizeG } = nutrition
  return (
    <table className="w-full text-xs border border-slate-200">
      <thead>
        <tr className="bg-slate-50 border-b border-slate-200">
          <th className="text-left px-2 py-1.5 font-medium text-slate-600">Nutriente</th>
          <th className="text-right px-2 py-1.5 font-medium text-slate-600">/ 100 g</th>
          <th className="text-right px-2 py-1.5 font-medium text-slate-600">/ porción ({servingSizeG} g)</th>
        </tr>
      </thead>
      <tbody>
        {[
          { label: 'Energía', c100: per100g.energyKcal, por: perPortion.energyKcal, unit: 'kcal' },
          { label: 'Proteínas', c100: per100g.proteinsG, por: perPortion.proteinsG, unit: 'g' },
          { label: 'Carbohidratos', c100: per100g.carbsG, por: perPortion.carbsG, unit: 'g' },
          { label: 'Grasas totales', c100: per100g.fatTotalG, por: perPortion.fatTotalG, unit: 'g' },
          { label: 'Sodio', c100: per100g.sodiumMg, por: perPortion.sodiumMg, unit: 'mg' },
        ].map(r => (
          <tr key={r.label} className="border-b border-slate-100 last:border-0">
            <td className="px-2 py-1.5 text-slate-700">{r.label}</td>
            <td className="px-2 py-1.5 text-right tabular-nums">{r.c100} {r.unit}</td>
            <td className="px-2 py-1.5 text-right tabular-nums">{r.por} {r.unit}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
