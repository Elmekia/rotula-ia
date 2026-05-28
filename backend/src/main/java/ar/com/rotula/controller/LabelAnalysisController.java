package ar.com.rotula.controller;

import ar.com.rotula.service.LabelAnalysisResult;
import ar.com.rotula.service.LabelAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint de análisis de rótulo para un producto.
 *
 * <pre>
 *   GET /products/{id}/analysis
 * </pre>
 *
 * Retorna:
 * <ul>
 *   <li>Tabla nutricional calculada por porción y por 100 g/mL (CAA Art. 1354)</li>
 *   <li>Sellos de advertencia que corresponden (Ley 27.642 / Decreto 151/2022)</li>
 *   <li>Declaración de alérgenos formateada (Res. 109/2023)</li>
 * </ul>
 */
@RestController
@RequestMapping("/products/{id}/analysis")
@RequiredArgsConstructor
public class LabelAnalysisController {

    private final LabelAnalysisService analysisService;

    /**
     * Analiza el rótulo del producto identificado por {@code id}.
     * El producto debe pertenecer al tenant del usuario autenticado.
     */
    @GetMapping
    public LabelAnalysisResult analyze(@PathVariable UUID id) {
        return analysisService.analyze(id);
    }
}
