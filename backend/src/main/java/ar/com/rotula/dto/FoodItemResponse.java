package ar.com.rotula.dto;

import ar.com.rotula.domain.FoodItem;

import java.math.BigDecimal;
import java.util.UUID;

public record FoodItemResponse(
        UUID       id,
        UUID       foodGroupId,
        String     name,
        /** Porción de referencia en gramos o mL (TABLA I – Res. Conjunta 21/2023). */
        BigDecimal portionGrams,
        /** Unidad de la porción: 'g' o 'ml'. */
        String     unit,
        int        sortOrder
) {
    public static FoodItemResponse from(FoodItem i) {
        return new FoodItemResponse(
                i.getId(),
                i.getFoodGroupId(),
                i.getName(),
                i.getPortionGrams(),
                i.getUnit(),
                i.getSortOrder()
        );
    }
}
