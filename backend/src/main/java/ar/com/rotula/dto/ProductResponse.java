package ar.com.rotula.dto;

import ar.com.rotula.domain.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID tenantId,
        String name,
        String category,
        BigDecimal netWeight,
        String weightUnit,
        String rneNumber,
        String rnpaNumber,
        String status,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getTenantId(),
                p.getName(),
                p.getCategory(),
                p.getNetWeight(),
                p.getWeightUnit(),
                p.getRneNumber(),
                p.getRnpaNumber(),
                p.getStatus(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
