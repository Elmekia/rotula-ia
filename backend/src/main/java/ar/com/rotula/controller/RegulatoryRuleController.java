package ar.com.rotula.controller;

import ar.com.rotula.dto.RegulatoryRuleResponse;
import ar.com.rotula.service.RegulatoryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de solo lectura para las reglas normativas del CAA / Ley 27.642.
 *
 * Endpoints:
 *   GET /regulatory-rules              → todas las reglas activas
 *   GET /regulatory-rules?category=X   → filtrar por categoría
 *   GET /regulatory-rules/{code}        → una regla por código
 */
@RestController
@RequestMapping("/regulatory-rules")
@RequiredArgsConstructor
public class RegulatoryRuleController {

    private final RegulatoryRuleService ruleService;

    /**
     * Lista todas las reglas activas, con filtro opcional por categoría.
     *
     * @param category opcional — filtra por categoría de producto
     *                 (ej.: bebidas, lacteos, panificados, conservas, snacks)
     */
    @GetMapping
    public List<RegulatoryRuleResponse> list(
            @RequestParam(required = false) String category) {
        return ruleService.findActive(category);
    }

    /**
     * Obtiene una regla activa por su código único (ej.: BEB-001).
     */
    @GetMapping("/{code}")
    public RegulatoryRuleResponse getByCode(@PathVariable String code) {
        return ruleService.findByCode(code);
    }
}
