package ar.com.rotula.dto.label;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de la generación de un rótulo o de la consulta de una versión.
 *
 * @param id          ID del registro en {@code label_projects}.
 * @param productId   ID del producto al que pertenece el rótulo.
 * @param version     Número de versión (auto-incremental por producto).
 * @param status      Estado del rótulo: draft, approved, exported.
 * @param labelData   Contenido completo del rótulo generado.
 * @param generatedAt Fecha y hora de generación.
 */
public record LabelVersionResponse(
        UUID id,
        UUID productId,
        int version,
        String status,
        LabelDTO labelData,
        OffsetDateTime generatedAt
) {}
