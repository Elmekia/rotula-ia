package ar.com.rotula.dto;

import ar.com.rotula.domain.Ingredient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IngredientResponse(
        UUID id,
        UUID productId,
        UUID tenantId,
        String name,
        BigDecimal percentage,
        boolean allergen,
        int sortOrder,
        OffsetDateTime createdAt
) {
    public static IngredientResponse from(Ingredient i) {
        return new IngredientResponse(
                i.getId(),
                i.getProductId(),
                i.getTenantId(),
                i.getName(),
                i.getPercentage(),
                i.isAllergen(),
                i.getSortOrder(),
                i.getCreatedAt()
        );
    }
}
