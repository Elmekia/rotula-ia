package ar.com.rotula.dto.label;

import java.math.BigDecimal;

/**
 * Representación simplificada de un ingrediente para el rótulo generado.
 * Los ingredientes están ordenados por peso decreciente (mayor a menor porcentaje).
 */
public record IngredientItem(
        String name,
        BigDecimal weightGrams,
        BigDecimal percentage,
        boolean allergen
) {}
