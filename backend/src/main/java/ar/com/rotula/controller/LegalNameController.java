package ar.com.rotula.controller;

import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.service.LegalNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Sugiere la denominación legal de venta de un producto según su categoría
 * y composición (CAA Art. 1385 / tabla {@code regulatory_name}).
 *
 * <pre>
 *   GET /products/{id}/legal-name
 * </pre>
 */
@RestController
@RequestMapping("/products/{id}/legal-name")
@RequiredArgsConstructor
public class LegalNameController {

    private final LegalNameService legalNameService;

    @GetMapping
    public LegalNameSuggestion suggest(@PathVariable UUID id) {
        return legalNameService.suggest(id);
    }
}
