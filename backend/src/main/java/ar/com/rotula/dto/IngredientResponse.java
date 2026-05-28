package ar.com.rotula.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de ingrediente.
 * {@code weightGrams} es el valor ingresado por el usuario.
 * {@code percentage} es calculado por el servicio como weightGrams / sum(weightGrams) * 100.
 */
public record IngredientResponse(
        UUID id,
        UUID productId,
        UUID tenantId,
        String name,
        BigDecimal weightGrams,
        BigDecimal percentage,
        boolean allergen,
        OffsetDateTime createdAt
) {}
