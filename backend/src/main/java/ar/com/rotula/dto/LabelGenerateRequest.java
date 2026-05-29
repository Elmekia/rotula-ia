package ar.com.rotula.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body del endpoint {@code POST /labels/generate}.
 *
 * @param productId ID del producto para el que se genera el rótulo.
 */
public record LabelGenerateRequest(
        @NotNull UUID productId
) {}
