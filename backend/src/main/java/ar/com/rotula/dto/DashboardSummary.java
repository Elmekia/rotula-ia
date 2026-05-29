package ar.com.rotula.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resumen del dashboard: contadores globales + últimos rótulos generados.
 *
 * @param totalProducts      Total de productos del tenant.
 * @param totalLabels        Total de versiones de rótulos generadas.
 * @param pendingReviewLabels Rótulos en estado «draft» (pendientes de revisión).
 * @param approvedLabels     Rótulos en estado «approved».
 * @param recentLabels       Últimos 5 rótulos generados, más reciente primero.
 */
public record DashboardSummary(
        long totalProducts,
        long totalLabels,
        long pendingReviewLabels,
        long approvedLabels,
        List<RecentLabelEntry> recentLabels
) {
    /**
     * Entrada en la lista de rótulos recientes.
     *
     * @param labelId          ID del registro {@code label_projects}.
     * @param productId        ID del producto.
     * @param productName      Nombre comercial del producto.
     * @param version          Número de versión.
     * @param status           Estado del rótulo.
     * @param legalDenomination Denominación legal sugerida en la generación.
     * @param generatedAt      Fecha de generación.
     */
    public record RecentLabelEntry(
            UUID labelId,
            UUID productId,
            String productName,
            int version,
            String status,
            String legalDenomination,
            OffsetDateTime generatedAt
    ) {}
}
