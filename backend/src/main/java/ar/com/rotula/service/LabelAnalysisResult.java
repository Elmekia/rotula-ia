package ar.com.rotula.service;

import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;

import java.util.Set;
import java.util.UUID;

/**
 * Resultado completo del análisis de rótulo para un producto.
 *
 * @param productId  ID del producto analizado
 * @param nutrition  Tabla nutricional calculada (null si no hay datos nutricionales)
 * @param seals      Sellos de advertencia que corresponden (vacío si ninguno)
 * @param allergens  Declaración de alérgenos
 */
public record LabelAnalysisResult(
        UUID productId,
        NutritionCalculationResult nutrition,
        Set<String> seals,
        AllergenDeclarationResult allergens
) {}
