package ar.com.rotula.dto;

import ar.com.rotula.domain.RegulatoryRule;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección pública de una regla normativa.
 * El campo {@code conditionDsl} contiene el JSON de aplicabilidad —
 * el cliente puede usarlo para pre-evaluar reglas sin llamar al backend.
 */
public record RegulatoryRuleResponse(
        UUID id,
        String code,
        String category,
        String description,
        String conditionDsl,
        String action,
        String actionDetail,
        String sourceArticle,
        LocalDate validFrom,
        LocalDate validUntil,
        int version,
        String notes,
        OffsetDateTime createdAt
) {
    public static RegulatoryRuleResponse from(RegulatoryRule r) {
        return new RegulatoryRuleResponse(
                r.getId(),
                r.getCode(),
                r.getCategory(),
                r.getDescription(),
                r.getConditionDsl(),
                r.getAction(),
                r.getActionDetail(),
                r.getSourceArticle(),
                r.getValidFrom(),
                r.getValidUntil(),
                r.getVersion(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
