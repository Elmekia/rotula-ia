package ar.com.rotula.dto;

import ar.com.rotula.domain.Ingredient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de ingrediente.
 * {@code weightGrams} es el valor ingresado por el usuario.
 * {@code percentage} es calculado por el servicio como weightGrams / sum(weightGrams) * 100.
 * Los campos nutricionales son los valores por 100 g del ingrediente (pueden ser null).
 */
public record IngredientResponse(
        UUID id,
        UUID productId,
        UUID tenantId,
        String name,
        BigDecimal weightGrams,
        BigDecimal percentage,
        boolean allergen,
        OffsetDateTime createdAt,
        BigDecimal energyKcalPer100g,
        BigDecimal proteinsPer100g,
        BigDecimal carbsPer100g,
        BigDecimal sugarsPer100g,
        BigDecimal fatTotalPer100g,
        BigDecimal fatSatPer100g,
        BigDecimal fatTransPer100g,
        BigDecimal sodiumMgPer100g
) {
    /**
     * Construye el response a partir de la entidad, con el porcentaje ya calculado.
     */
    public static IngredientResponse from(Ingredient i, BigDecimal percentage) {
        return new IngredientResponse(
                i.getId(),
                i.getProductId(),
                i.getTenantId(),
                i.getName(),
                i.getWeightGrams(),
                percentage,
                i.isAllergen(),
                i.getCreatedAt(),
                i.getEnergyKcalPer100g(),
                i.getProteinsPer100g(),
                i.getCarbsPer100g(),
                i.getSugarsPer100g(),
                i.getFatTotalPer100g(),
                i.getFatSatPer100g(),
                i.getFatTransPer100g(),
                i.getSodiumMgPer100g()
        );
    }
}
