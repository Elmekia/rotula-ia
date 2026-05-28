package ar.com.rotula.service.nutrition;

/**
 * Valores nutricionales ya redondeados según CAA Art. 1354, listos para
 * imprimir en la tabla nutricional del rótulo.
 *
 * <p>Energía expresada como entero (kcal/kJ). Macronutrientes en gramos (enteros).
 * Grasas trans con un decimal (0.5 g) cuando aplica. Sodio en mg (entero).
 */
public record RoundedNutritionValues(
        int    energyKcal,
        int    energyKj,
        int    proteinsG,
        int    carbsG,
        int    sugarsG,
        int    fatTotalG,
        int    fatSatG,
        double fatTransG,
        int    sodiumMg
) {}
