import { AlertTriangle, Info } from 'lucide-react'
import { OctagonSeal } from './OctagonSeal'
import type { LabelDTO } from '../../types/label'
import type { RoundedNutritionValues } from '../../types/product'

interface Props {
  label: LabelDTO
}

/**
 * Preview del rótulo con layout normativo del CAA.
 *
 * Secciones renderizadas (orden normativo):
 *  1. Denominación de venta + alerta si difiere de la legal
 *  2. Sellos de advertencia (panel frontal, Ley 27.642)
 *  3. Contenido neto
 *  4. Lista de ingredientes
 *  5. Tabla nutricional (CAA Art. 1354)
 *  6. Declaración de alérgenos (Res. 109/2023)
 *  7. Claims nutricionales (CAA Art. 1385)
 *  8. RNE / RNPA
 */
export function LabelPreview({ label }: Props) {
  const {
    productName, legalDenomination, category, netWeight, weightUnit,
    rneNumber, rnpaNumber, ingredients, nutrition, seals, allergens,
    claims, legalNameAlert,
  } = label

  return (
    <div className="bg-white border-2 border-slate-800 rounded-sm max-w-md mx-auto font-sans text-slate-900 text-xs shadow-lg">

      {/* ── Panel frontal ──────────────────────────────────────────────────── */}
      <div className="border-b-2 border-slate-800 p-4">

        {/* Alerta de denominación legal */}
        {legalNameAlert && legalDenomination && (
          <div className="flex items-start gap-1.5 mb-3 px-2 py-1.5 bg-amber-50 border border-amber-300 rounded text-amber-800 text-xs">
            <AlertTriangle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
            <span>
              <strong>Denominación sugerida:</strong> {legalDenomination}
            </span>
          </div>
        )}

        {/* Nombre del producto */}
        <h1 className="text-lg font-black uppercase tracking-wide text-center leading-tight">
          {productName}
        </h1>

        {/* Denominación legal */}
        {legalDenomination && (
          <p className="text-center text-xs text-slate-600 mt-0.5">
            {legalDenomination}
          </p>
        )}

        {/* Categoría */}
        <p className="text-center text-xs text-slate-400 italic">{category}</p>

        {/* Sellos de advertencia — panel frontal obligatorio */}
        {seals.length > 0 && (
          <div className="flex flex-wrap gap-2 justify-center mt-4">
            {seals.map(seal => (
              <OctagonSeal key={seal} label={seal} />
            ))}
          </div>
        )}

        {/* Contenido neto */}
        <p className="text-center font-bold mt-3 text-sm">
          Contenido neto: {netWeight} {weightUnit}
        </p>
      </div>

      {/* ── Panel lateral / dorso ──────────────────────────────────────────── */}
      <div className="p-4 space-y-4">

        {/* Ingredientes */}
        <section>
          <SectionTitle>INGREDIENTES</SectionTitle>
          {ingredients.length === 0 ? (
            <p className="text-slate-400 italic">Sin ingredientes cargados.</p>
          ) : (
            <p className="leading-relaxed">
              {ingredients
                .map(i => `${i.name} (${Number(i.percentage).toFixed(1)}%)`)
                .join(', ')}.
            </p>
          )}
        </section>

        {/* Tabla nutricional */}
        {nutrition ? (
          <section>
            <SectionTitle>INFORMACIÓN NUTRICIONAL</SectionTitle>
            <p className="text-slate-500 mb-1">
              Redondeada según CAA Art. 1354
            </p>
            <NutritionTable
              per100g={nutrition.per100g}
              perPortion={nutrition.perPortion}
              servingSizeG={nutrition.servingSizeG}
            />
          </section>
        ) : (
          <section>
            <SectionTitle>INFORMACIÓN NUTRICIONAL</SectionTitle>
            <div className="flex items-center gap-1.5 text-slate-400 text-xs">
              <Info className="w-3.5 h-3.5" />
              <span>Cargá datos nutricionales en los ingredientes y una porción de referencia.</span>
            </div>
          </section>
        )}

        {/* Alérgenos */}
        {(allergens.declarationText) && (
          <section>
            <SectionTitle>ALÉRGENOS</SectionTitle>
            <p className="font-medium leading-relaxed">{allergens.declarationText}</p>
          </section>
        )}

        {/* Claims nutricionales */}
        {claims.length > 0 && (
          <section>
            <SectionTitle>INFORMACIÓN ADICIONAL</SectionTitle>
            <ul className="space-y-0.5">
              {claims.map(c => (
                <li key={c} className="flex items-center gap-1">
                  <span className="w-1 h-1 rounded-full bg-slate-400 shrink-0" />
                  {c}
                </li>
              ))}
            </ul>
          </section>
        )}

        {/* RNE / RNPA */}
        {(rneNumber || rnpaNumber) && (
          <section className="border-t border-slate-200 pt-3 text-slate-500">
            {rneNumber  && <p>RNE: {rneNumber}</p>}
            {rnpaNumber && <p>RNPA: {rnpaNumber}</p>}
          </section>
        )}
      </div>
    </div>
  )
}

// ── Tabla nutricional ─────────────────────────────────────────────────────────

function NutritionTable({
  per100g, perPortion, servingSizeG,
}: {
  per100g: RoundedNutritionValues
  perPortion: RoundedNutritionValues
  servingSizeG: number
}) {
  const rows: { label: string; unit: string; c100: number | string; portion: number | string }[] = [
    { label: 'Energía',                   unit: 'kcal', c100: per100g.energyKcal,  portion: perPortion.energyKcal },
    { label: 'Energía',                   unit: 'kJ',   c100: per100g.energyKj,    portion: perPortion.energyKj },
    { label: 'Proteínas',                 unit: 'g',    c100: per100g.proteinsG,   portion: perPortion.proteinsG },
    { label: 'Carbohidratos totales',     unit: 'g',    c100: per100g.carbsG,      portion: perPortion.carbsG },
    { label: '  — Azúcares',             unit: 'g',    c100: per100g.sugarsG,     portion: perPortion.sugarsG },
    { label: 'Grasas totales',            unit: 'g',    c100: per100g.fatTotalG,   portion: perPortion.fatTotalG },
    { label: '  — Saturadas',            unit: 'g',    c100: per100g.fatSatG,     portion: perPortion.fatSatG },
    { label: '  — Trans',               unit: 'g',    c100: per100g.fatTransG,   portion: perPortion.fatTransG },
    { label: 'Sodio',                     unit: 'mg',   c100: per100g.sodiumMg,    portion: perPortion.sodiumMg },
  ]

  return (
    <table className="w-full border border-slate-800 text-xs">
      <thead>
        <tr className="border-b border-slate-800 bg-slate-100">
          <th className="text-left p-1 font-bold">Nutriente</th>
          <th className="text-right p-1 font-bold whitespace-nowrap">Por 100 g</th>
          <th className="text-right p-1 font-bold whitespace-nowrap">Por porción ({servingSizeG} g)</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r, i) => (
          <tr key={i} className="border-b border-slate-200 last:border-0">
            <td className="p-1">{r.label}</td>
            <td className="p-1 text-right tabular-nums">{r.c100} {r.unit}</td>
            <td className="p-1 text-right tabular-nums">{r.portion} {r.unit}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

// ── Shared ────────────────────────────────────────────────────────────────────

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="font-black text-xs uppercase tracking-widest border-b border-slate-800 pb-0.5 mb-1.5">
      {children}
    </h3>
  )
}
