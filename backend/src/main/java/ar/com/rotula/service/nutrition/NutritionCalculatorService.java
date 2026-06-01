package ar.com.rotula.service.nutrition;

import ar.com.rotula.domain.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Calcula la tabla nutricional de un producto a partir de los valores por 100 g
 * cargados directamente en el producto (CAA Art. 1354).
 *
 * <p>Fórmula: valor_por_porción = valor_por_100g × serving_size_g / 100
 *
 * <p>Retorna {@code null} si el producto no tiene datos nutricionales o no tiene
 * porción de referencia definida.
 */
@Service
public class NutritionCalculatorService {

    private static final double KJ_PER_KCAL = 4.184;

    /**
     * Calcula nutrición a partir de los campos por 100 g del producto.
     *
     * @param product producto con los campos nutricionales y serving_size_g
     * @return resultado del cálculo, o {@code null} si no hay datos nutricionales
     */
    public NutritionCalculationResult calculate(Product product) {
        if (product.getServingSizeG() == null) return null;
        if (!hasAnyNutrition(product)) return null;

        double ss = product.getServingSizeG().doubleValue();

        double kcal   = nullToZero(product.getEnergyKcalPer100g());
        double prot   = nullToZero(product.getProteinsPer100g());
        double carbs  = nullToZero(product.getCarbsPer100g());
        double sugars = nullToZero(product.getSugarsPer100g());
        double fat    = nullToZero(product.getFatTotalPer100g());
        double sat    = nullToZero(product.getFatSatPer100g());
        double trans  = nullToZero(product.getFatTransPer100g());
        double sodium = nullToZero(product.getSodiumMgPer100g());

        // Valores por 100 g (raw, sin redondear)
        NutritionValues raw100g = build(kcal, prot, carbs, sugars, fat, sat, trans, sodium);

        // Valores por porción = campo_por_100g × serving_size_g / 100
        NutritionValues rawPortion = build(
                kcal   * ss / 100,
                prot   * ss / 100,
                carbs  * ss / 100,
                sugars * ss / 100,
                fat    * ss / 100,
                sat    * ss / 100,
                trans  * ss / 100,
                sodium * ss / 100
        );

        return new NutritionCalculationResult(
                product.getServingSizeG(),
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

    private static boolean hasAnyNutrition(Product p) {
        return p.getEnergyKcalPer100g() != null
            || p.getProteinsPer100g()   != null
            || p.getCarbsPer100g()      != null
            || p.getSugarsPer100g()     != null
            || p.getFatTotalPer100g()   != null
            || p.getFatSatPer100g()     != null
            || p.getFatTransPer100g()   != null
            || p.getSodiumMgPer100g()   != null;
    }
}
