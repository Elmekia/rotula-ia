package ar.com.rotula.service.nutrition;

import ar.com.rotula.domain.Ingredient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calcula la tabla nutricional de un producto a partir de la lista de
 * ingredientes con datos nutricionales por 100 g.
 *
 * <p>El cálculo sigue la metodología del CAA Art. 1354:
 * <ol>
 *   <li>Suma la contribución nutricional de cada ingrediente
 *       (campo_per100g × weightGrams / 100).</li>
 *   <li>Divide el total por el peso total del producto para obtener valores
 *       por 100 g del producto.</li>
 *   <li>Escala por el tamaño de porción para obtener valores por porción.</li>
 *   <li>Aplica el redondeo según {@link NutritionRounder}.</li>
 * </ol>
 *
 * <p>Retorna {@code null} si ningún ingrediente tiene datos nutricionales cargados.
 */
@Service
public class NutritionCalculatorService {

    private static final double KJ_PER_KCAL = 4.184;

    /**
     * @param ingredients  lista de ingredientes del producto
     * @param totalWeightG suma de weightGrams de todos los ingredientes
     * @param servingSizeG tamaño de porción en gramos (o mL)
     * @return resultado del cálculo, o {@code null} si no hay datos nutricionales
     */
    public NutritionCalculationResult calculate(
            List<Ingredient> ingredients,
            BigDecimal totalWeightG,
            BigDecimal servingSizeG) {

        // Acumular contribución de cada ingrediente
        double energyKcal = 0, proteins = 0, carbs = 0, sugars = 0;
        double fatTotal = 0, fatSat = 0, fatTrans = 0, sodium = 0;
        boolean hasData = false;

        for (Ingredient i : ingredients) {
            if (i.getEnergyKcalPer100g() == null) continue;
            hasData = true;

            double w = i.getWeightGrams().doubleValue() / 100.0;
            energyKcal += nullToZero(i.getEnergyKcalPer100g()) * w;
            proteins   += nullToZero(i.getProteinsPer100g())    * w;
            carbs      += nullToZero(i.getCarbsPer100g())        * w;
            sugars     += nullToZero(i.getSugarsPer100g())       * w;
            fatTotal   += nullToZero(i.getFatTotalPer100g())     * w;
            fatSat     += nullToZero(i.getFatSatPer100g())       * w;
            fatTrans   += nullToZero(i.getFatTransPer100g())     * w;
            sodium     += nullToZero(i.getSodiumMgPer100g())     * w;
        }

        if (!hasData) return null;

        double tw = totalWeightG.doubleValue();
        double ss = servingSizeG.doubleValue();

        // Valores por 100 g del producto (sin redondear)
        NutritionValues raw100g = build(
                energyKcal / tw * 100,
                proteins   / tw * 100,
                carbs      / tw * 100,
                sugars     / tw * 100,
                fatTotal   / tw * 100,
                fatSat     / tw * 100,
                fatTrans   / tw * 100,
                sodium     / tw * 100
        );

        // Valores por porción (sin redondear)
        NutritionValues rawPortion = build(
                energyKcal / tw * ss,
                proteins   / tw * ss,
                carbs      / tw * ss,
                sugars     / tw * ss,
                fatTotal   / tw * ss,
                fatSat     / tw * ss,
                fatTrans   / tw * ss,
                sodium     / tw * ss
        );

        return new NutritionCalculationResult(
                servingSizeG,
                NutritionRounder.round(rawPortion),
                NutritionRounder.round(raw100g),
                raw100g
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static NutritionValues build(
            double kcal, double prot, double carb, double sug,
            double fat, double sat, double trans, double na) {
        return new NutritionValues(kcal, kcal * KJ_PER_KCAL, prot, carb, sug, fat, sat, trans, na);
    }

    private static double nullToZero(BigDecimal v) {
        return v == null ? 0.0 : v.doubleValue();
    }
}
