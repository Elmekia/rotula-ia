package ar.com.rotula.service.nutrition;

import java.math.BigDecimal;

/**
 * Resultado del cálculo de la tabla nutricional de un producto.
 *
 * <p>Contiene los valores redondeados según CAA Art. 1354 por porción y
 * por 100 g/mL, más los valores crudos por 100 g usados internamente
 * para la evaluación de sellos de advertencia (Ley 27.642).
 */
public record NutritionCalculationResult(

        /** Tamaño de porción utilizado en el cálculo (en g o mL). */
        BigDecimal servingSizeG,

        /** Valores redondeados por porción (para la tabla nutricional del rótulo). */
        RoundedNutritionValues perPortion,

        /** Valores redondeados por 100 g / 100 mL (para la tabla nutricional del rótulo). */
        RoundedNutritionValues per100g,

        /**
         * Valores crudos (sin redondeo) por 100 g / 100 mL.
         * Utilizados para evaluar si corresponden sellos de advertencia según Ley 27.642
         * (los umbrales se comparan contra valores sin redondear).
         */
        NutritionValues rawPer100g
) {}
