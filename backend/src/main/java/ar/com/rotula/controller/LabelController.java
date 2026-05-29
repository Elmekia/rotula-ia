package ar.com.rotula.controller;

import ar.com.rotula.dto.LabelGenerateRequest;
import ar.com.rotula.dto.label.LabelVersionResponse;
import ar.com.rotula.dto.label.LabelVersionSummary;
import ar.com.rotula.service.LabelGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Generación y consulta de versiones de rótulos.
 *
 * <pre>
 *   POST /labels/generate                  → genera nueva versión
 *   GET  /labels/{productId}/versions      → historial de versiones
 * </pre>
 */
@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelGenerationService labelGenerationService;

    /**
     * Genera un rótulo completo para el producto indicado y lo persiste
     * como una nueva versión (version++).
     *
     * @return {@link LabelVersionResponse} con el rótulo completo y el número de versión asignado.
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public LabelVersionResponse generate(@Valid @RequestBody LabelGenerateRequest request) {
        return labelGenerationService.generate(request.productId());
    }

    /**
     * Devuelve el historial de versiones de rótulos para un producto,
     * ordenado de la más reciente a la más antigua.
     *
     * @param productId ID del producto.
     */
    @GetMapping("/{productId}/versions")
    public List<LabelVersionSummary> getVersions(@PathVariable UUID productId) {
        return labelGenerationService.getVersions(productId);
    }
}
