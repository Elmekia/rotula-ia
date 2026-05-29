package ar.com.rotula.dto;

import java.util.List;

/**
 * Sugerencia de denominación legal de venta según el CAA.
 *
 * @param suggestedName  Denominación legal base según la categoría del producto
 *                       (null si la categoría no está en la tabla de denominaciones).
 * @param modifiers      Claims nutricionales aplicables al producto según CAA Art. 1385
 *                       (ej. "Bajo en sodio", "Sin azúcares", "Enriquecido con calcio").
 * @param alertDiffers   {@code true} si la denominación comercial del producto no coincide
 *                       con la denominación legal sugerida.
 * @param sourceArticle  Artículo o resolución normativa que fundamenta la sugerencia.
 */
public record LegalNameSuggestion(
        String suggestedName,
        List<String> modifiers,
        boolean alertDiffers,
        String sourceArticle
) {}
