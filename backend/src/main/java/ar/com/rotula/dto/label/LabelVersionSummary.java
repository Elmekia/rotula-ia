package ar.com.rotula.dto.label;

import ar.com.rotula.domain.LabelProject;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resumen de una versión de rótulo para el historial.
 *
 * @param id                ID del registro.
 * @param productId         ID del producto.
 * @param version           Número de versión.
 * @param status            Estado: draft, approved, exported.
 * @param legalDenomination Denominación legal sugerida en el momento de la generación.
 * @param generatedAt       Fecha y hora de generación.
 */
public record LabelVersionSummary(
        UUID id,
        UUID productId,
        int version,
        String status,
        String legalDenomination,
        OffsetDateTime generatedAt
) {
    public static LabelVersionSummary from(LabelProject lp) {
        return new LabelVersionSummary(
                lp.getId(),
                lp.getProductId(),
                lp.getVersion(),
                lp.getStatus(),
                lp.getLegalDenomination(),
                lp.getCreatedAt()
        );
    }
}
