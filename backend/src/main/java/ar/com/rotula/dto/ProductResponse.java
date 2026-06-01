package ar.com.rotula.dto;

import ar.com.rotula.domain.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID   id,
        UUID   tenantId,
        String name,
        String denomination,

        // Clasificación TABLA I
        UUID   foodGroupId,
        UUID   foodItemId,
        BigDecimal servingSizeG,

        // Presentación
        BigDecimal netWeight,
        String     weightUnit,

        // Registros
        String rneNumber,
        String rnpaNumber,

        // Contaminación cruzada (lista deserializada)
        List<String> crossContaminationGroups,

        // Opciones de rótulo
        boolean showIngredientPercentages,

        // Tabla nutricional por 100 g
        BigDecimal energyKcalPer100g,
        BigDecimal proteinsPer100g,
        BigDecimal carbsPer100g,
        BigDecimal sugarsPer100g,
        BigDecimal fatTotalPer100g,
        BigDecimal fatSatPer100g,
        BigDecimal fatTransPer100g,
        BigDecimal sodiumMgPer100g,

        // Auditoría
        String         status,
        UUID           createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getTenantId(),
                p.getName(),
                p.getDenomination(),
                p.getFoodGroupId(),
                p.getFoodItemId(),
                p.getServingSizeG(),
                p.getNetWeight(),
                p.getWeightUnit(),
                p.getRneNumber(),
                p.getRnpaNumber(),
                parseCrossContamination(p.getCrossContamination()),
                p.isShowIngredientPercentages(),
                p.getEnergyKcalPer100g(),
                p.getProteinsPer100g(),
                p.getCarbsPer100g(),
                p.getSugarsPer100g(),
                p.getFatTotalPer100g(),
                p.getFatSatPer100g(),
                p.getFatTransPer100g(),
                p.getSodiumMgPer100g(),
                p.getStatus(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    /** Convierte "MANI,FRUTOS_DE_CASCARA" → ["MANI","FRUTOS_DE_CASCARA"]. */
    private static List<String> parseCrossContamination(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
