import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, FlaskConical, Loader2 } from 'lucide-react'
import { analysisApi } from '../../lib/analysisApi'
import type { LabelAnalysisResult, RoundedNutritionValues } from '../../types/product'

interface Props {
  productId: string
}

export function LabelAnalysisPanel({ productId }: Props) {
  const { data, isLoading, isError } = useQuery<LabelAnalysisResult>({
    queryKey: ['analysis', productId],
    queryFn:  () => analysisApi.analyze(productId),
    enabled:  !!productId,
    staleTime: 30_000,
  })

  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6 flex items-center gap-3 text-slate-400">
        <Loader2 className="w-5 h-5 animate-spin" />
        <span className="text-sm">Calculando análisis de rótulo…</span>
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="bg-white rounded-xl border border-red-100 p-6 text-sm text-red-500">
        No se pudo cargar el análisis de rótulo.
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* ── Tabla nutricional ──────────────────────────────────────────────── */}
      {data.nutrition ? (
        <NutritionTable nutrition={data.nutrition.per100g} perPortion={data.nutrition.perPortion} servingSizeG={data.nutrition.servingSizeG} />
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <SectionHeader icon={<FlaskConical className="w-4 h-4 text-slate-500" />} title="Tabla nutricional" />
          <p className="text-sm text-slate-400 mt-2">
            Cargá los datos nutricionales en los ingredientes y una porción de referencia en el producto para
            calcular la tabla.
          </p>
        </div>
      )}

      {/* ── Sellos de advertencia ──────────────────────────────────────────── */}
      <SealBadges seals={data.seals} />

      {/* ── Declaración de alérgenos ───────────────────────────────────────── */}
      <AllergenCard allergens={data.allergens} />
    </div>
  )
}

// ── Tabla nutricional ─────────────────────────────────────────────────────────

function NutritionTable({
  nutrition,
  perPortion,
  servingSizeG,
}: {
  nutrition:    RoundedNutritionValues
  perPortion:   RoundedNutritionValues
  servingSizeG: number
}) {
  const rows: { label: string; unit: string; per100: number | string; portion: number | string }[] = [
    { label: 'Energía',           unit: 'kcal',   per100: nutrition.energyKcal,  portion: perPortion.energyKcal },
    { label: 'Energía',           unit: 'kJ',     per100: nutrition.energyKj,    portion: perPortion.energyKj },
    { label: 'Proteínas',         unit: 'g',      per100: nutrition.proteinsG,   portion: perPortion.proteinsG },
    { label: 'Carbohidratos',     unit: 'g',      per100: nutrition.carbsG,      portion: perPortion.carbsG },
    { label: '  de los cuales azúcares', unit: 'g', per100: nutrition.sugarsG,  portion: perPortion.sugarsG },
    { label: 'Grasas totales',    unit: 'g',      per100: nutrition.fatTotalG,   portion: perPortion.fatTotalG },
    { label: '  saturadas',       unit: 'g',      per100: nutrition.fatSatG,     portion: perPortion.fatSatG },
    { label: '  trans',           unit: 'g',      per100: nutrition.fatTransG,   portion: perPortion.fatTransG },
    { label: 'Sodio',             unit: 'mg',     per100: nutrition.sodiumMg,    portion: perPortion.sodiumMg },
  ]

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <SectionHeader icon={<FlaskConical className="w-4 h-4 text-slate-500" />} title="Tabla nutricional" />
      <p className="text-xs text-slate-400 mt-1 mb-3">Redondeada según CAA Art. 1354</p>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100">
              <th className="text-left py-2 pr-3 font-medium text-slate-600 text-xs">Nutriente</th>
              <th className="text-right py-2 px-2 font-medium text-slate-600 text-xs whitespace-nowrap">
                Por 100 g
              </th>
              <th className="text-right py-2 pl-2 font-medium text-slate-600 text-xs whitespace-nowrap">
                Por porción ({servingSizeG} g)
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, idx) => (
              <tr key={idx} className="border-b border-slate-50 last:border-0">
                <td className="py-1.5 pr-3 text-slate-700 text-xs">{r.label}</td>
                <td className="py-1.5 px-2 text-right text-slate-700 tabular-nums text-xs">
                  {r.per100} {r.unit}
                </td>
                <td className="py-1.5 pl-2 text-right text-slate-700 tabular-nums text-xs">
                  {r.portion} {r.unit}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Sellos ────────────────────────────────────────────────────────────────────

const SEAL_COLORS: Record<string, string> = {
  'EXCESO EN GRASAS SATURADAS': 'bg-red-100 text-red-700 border-red-200',
  'EXCESO EN GRASAS TRANS':     'bg-orange-100 text-orange-700 border-orange-200',
  'EXCESO EN SODIO':            'bg-yellow-100 text-yellow-700 border-yellow-200',
  'EXCESO EN AZÚCARES':         'bg-purple-100 text-purple-700 border-purple-200',
  'EXCESO EN CALORÍAS':         'bg-rose-100 text-rose-700 border-rose-200',
}

function SealBadges({ seals }: { seals: string[] }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <SectionHeader
        icon={<AlertTriangle className="w-4 h-4 text-slate-500" />}
        title="Sellos de advertencia"
      />
      <p className="text-xs text-slate-400 mt-1 mb-3">Según Ley 27.642 / Decreto 151/2022</p>

      {seals.length === 0 ? (
        <p className="text-sm text-emerald-600 font-medium">Sin sellos de advertencia</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {seals.map((seal) => (
            <span
              key={seal}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-xs font-semibold
                          ${SEAL_COLORS[seal] ?? 'bg-slate-100 text-slate-700 border-slate-200'}`}
            >
              <AlertTriangle className="w-3 h-3" />
              {seal}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Alérgenos ─────────────────────────────────────────────────────────────────

const ALLERGEN_LABELS: Record<string, string> = {
  GLUTEN:            'Gluten',
  CRUSTACEOS:        'Crustáceos',
  HUEVO:             'Huevo',
  PESCADO:           'Pescado',
  MANI:              'Maní',
  SOJA:              'Soja',
  LECHE:             'Leche',
  FRUTOS_DE_CASCARA: 'Frutos de cáscara',
  APIO:              'Apio',
  MOSTAZA:           'Mostaza',
  SESAMO:            'Sésamo',
  SULFITOS:          'Dióxido de azufre y sulfitos',
  ALTRAMUCES:        'Altramuces',
  MOLUSCOS:          'Moluscos',
}

function AllergenCard({ allergens }: { allergens: LabelAnalysisResult['allergens'] }) {
  const { presentGroups, crossContaminationGroups, declarationText } = allergens
  const isEmpty = presentGroups.length === 0 && crossContaminationGroups.length === 0

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <SectionHeader
        icon={<AlertTriangle className="w-4 h-4 text-amber-500" />}
        title="Declaración de alérgenos"
      />
      <p className="text-xs text-slate-400 mt-1 mb-3">Según Res. 109/2023 (14 grupos)</p>

      {isEmpty ? (
        <p className="text-sm text-slate-500">No se detectaron alérgenos ni contaminación cruzada.</p>
      ) : (
        <div className="space-y-3">
          {presentGroups.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-slate-600 mb-1.5">Contiene</p>
              <div className="flex flex-wrap gap-1.5">
                {presentGroups.map((g) => (
                  <span key={g} className="px-2.5 py-1 text-xs bg-amber-50 text-amber-700 border border-amber-200 rounded-full font-medium">
                    {ALLERGEN_LABELS[g] ?? g}
                  </span>
                ))}
              </div>
            </div>
          )}

          {crossContaminationGroups.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-slate-600 mb-1.5">Puede contener trazas de</p>
              <div className="flex flex-wrap gap-1.5">
                {crossContaminationGroups.map((g) => (
                  <span key={g} className="px-2.5 py-1 text-xs bg-slate-50 text-slate-600 border border-slate-200 rounded-full">
                    {ALLERGEN_LABELS[g] ?? g}
                  </span>
                ))}
              </div>
            </div>
          )}

          {declarationText && (
            <div className="mt-3 pt-3 border-t border-slate-100">
              <p className="text-xs font-semibold text-slate-600 mb-1">Texto para el rótulo</p>
              <p className="text-xs text-slate-700 bg-slate-50 rounded-lg px-3 py-2 font-medium leading-relaxed">
                {declarationText}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── shared ────────────────────────────────────────────────────────────────────

function SectionHeader({ icon, title }: { icon: React.ReactNode; title: string }) {
  return (
    <div className="flex items-center gap-2">
      {icon}
      <h3 className="text-sm font-semibold text-slate-700">{title}</h3>
    </div>
  )
}
