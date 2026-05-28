package ar.com.rotula.service.nutrition;

/**
 * Valores nutricionales crudos (sin redondeo según CAA Art. 1354).
 * Se expresa en las unidades estándar: kcal, kJ, gramos y miligramos.
 *
 * @param energyKcal   Energía en kilocalorías
 * @param energyKj     Energía en kilojulios (1 kcal = 4.184 kJ)
 * @param proteinsG    Proteínas en gramos
 * @param carbsG       Carbohidratos totales en gramos
 * @param sugarsG      Azúcares en gramos
 * @param fatTotalG    Grasas totales en gramos
 * @param fatSatG      Grasas saturadas en gramos
 * @param fatTransG    Grasas trans en gramos
 * @param sodiumMg     Sodio en miligramos
 */
public record NutritionValues(
        double energyKcal,
        double energyKj,
        double proteinsG,
        double carbsG,
        double sugarsG,
        double fatTotalG,
        double fatSatG,
        double fatTransG,
        double sodiumMg
) {
    public static NutritionValues ZERO = new NutritionValues(0, 0, 0, 0, 0, 0, 0, 0, 0);
}
