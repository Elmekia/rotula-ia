package ar.com.rotula.service.nutrition;

/**
 * Aplica las reglas de redondeo del CAA Art. 1354 (Res. Conjunta 26/2004
 * y modificatorias) a los valores nutricionales crudos.
 *
 * <pre>
 * Nutriente          Condición               Resultado
 * ─────────────────  ──────────────────────  ─────────────────────────────
 * Energía (kcal)     < 5                     0
 *                    5 ≤ x ≤ 50              múltiplo de 5 más cercano
 *                    > 50                    múltiplo de 10 más cercano
 *
 * Energía (kJ)       < 20                    0
 *                    20 ≤ x ≤ 210            múltiplo de 20 más cercano
 *                    > 210                   múltiplo de 40 más cercano
 *
 * Proteínas (g)      < 0.5                   0
 *                    ≥ 0.5                   redondeo a 1 g
 *
 * Carbohidratos (g)  < 0.5                   0
 *                    ≥ 0.5                   redondeo a 1 g
 *
 * Azúcares (g)       < 0.5                   0
 *                    ≥ 0.5                   redondeo a 1 g
 *
 * Grasas totales (g) < 0.5                   0
 *                    ≥ 0.5                   redondeo a 1 g
 *
 * Grasas sat. (g)    < 0.5                   0
 *                    ≥ 0.5                   redondeo a 1 g
 *
 * Grasas trans (g)   < 0.1                   0
 *                    0.1 ≤ x ≤ 2             múltiplo de 0.5 más cercano
 *                    > 2                     redondeo a 1 g
 *
 * Sodio (mg)         < 5                     0
 *                    5 ≤ x ≤ 140             múltiplo de 5 más cercano
 *                    > 140                   múltiplo de 10 más cercano
 * </pre>
 */
public final class NutritionRounder {

    private NutritionRounder() {}

    /**
     * Aplica el redondeo CAA a todos los valores y retorna un nuevo
     * {@link RoundedNutritionValues} con los valores redondeados.
     */
    public static RoundedNutritionValues round(NutritionValues raw) {
        return new RoundedNutritionValues(
                roundEnergy(raw.energyKcal()),
                roundEnergyKj(raw.energyKj()),
                roundGrams(raw.proteinsG()),
                roundGrams(raw.carbsG()),
                roundGrams(raw.sugarsG()),
                roundGrams(raw.fatTotalG()),
                roundGrams(raw.fatSatG()),
                roundTrans(raw.fatTransG()),
                roundSodium(raw.sodiumMg())
        );
    }

    // ── Reglas de redondeo ───────────────────────────────────────────────────

    /** Energía en kcal: <5→0, 5-50→múltiplo de 5, >50→múltiplo de 10. */
    public static int roundEnergy(double kcal) {
        if (kcal < 5) return 0;
        if (kcal <= 50) return roundToMultiple(kcal, 5);
        return roundToMultiple(kcal, 10);
    }

    /** Energía en kJ: <20→0, 20-210→múltiplo de 20, >210→múltiplo de 40. */
    public static int roundEnergyKj(double kj) {
        if (kj < 20) return 0;
        if (kj <= 210) return roundToMultiple(kj, 20);
        return roundToMultiple(kj, 40);
    }

    /** Proteínas, carbohidratos, azúcares, grasas totales, grasas saturadas en g: <0.5→0, ≥0.5→redondeo 1 g. */
    public static int roundGrams(double grams) {
        if (grams < 0.5) return 0;
        return (int) Math.round(grams);
    }

    /**
     * Grasas trans en g: <0.1→0, 0.1-2→múltiplo de 0.5, >2→redondeo a 1 g.
     * El resultado se expresa con un decimal para 0.5 y 1.5 pero como entero para ≥ 2.
     */
    public static double roundTrans(double trans) {
        if (trans < 0.1) return 0.0;
        if (trans <= 2.0) return Math.round(trans * 2.0) / 2.0;   // múltiplo de 0.5
        return Math.round(trans);                                   // redondeo a 1 g
    }

    /** Sodio en mg: <5→0, 5-140→múltiplo de 5, >140→múltiplo de 10. */
    public static int roundSodium(double mg) {
        if (mg < 5) return 0;
        if (mg <= 140) return roundToMultiple(mg, 5);
        return roundToMultiple(mg, 10);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static int roundToMultiple(double value, int multiple) {
        return (int) (Math.round(value / multiple) * multiple);
    }
}
