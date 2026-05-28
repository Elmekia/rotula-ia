package ar.com.rotula.service;

import ar.com.rotula.dto.RegulatoryRuleResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.RegulatoryRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegulatoryRuleService {

    private final RegulatoryRuleRepository ruleRepository;

    /**
     * Retorna todas las reglas activas.
     * Si se provee {@code category} (no nulo, no vacío), filtra solo las reglas
     * de esa categoría. De lo contrario retorna todas.
     */
    @Transactional(readOnly = true)
    public List<RegulatoryRuleResponse> findActive(String category) {
        if (category != null && !category.isBlank()) {
            return ruleRepository
                    .findByCategoryAndActiveTrueOrderByCodeAsc(category.toLowerCase().trim())
                    .stream()
                    .map(RegulatoryRuleResponse::from)
                    .toList();
        }
        return ruleRepository
                .findByActiveTrueOrderByCategoryAscCodeAsc()
                .stream()
                .map(RegulatoryRuleResponse::from)
                .toList();
    }

    /**
     * Retorna una regla activa por código.
     *
     * @throws ResourceNotFoundException si no existe ninguna regla activa con ese código
     */
    @Transactional(readOnly = true)
    public RegulatoryRuleResponse findByCode(String code) {
        return ruleRepository.findByCodeAndActiveTrue(code.toUpperCase().trim())
                .map(RegulatoryRuleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regla normativa no encontrada: " + code));
    }
}
