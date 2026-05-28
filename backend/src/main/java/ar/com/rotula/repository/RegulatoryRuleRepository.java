package ar.com.rotula.repository;

import ar.com.rotula.domain.RegulatoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegulatoryRuleRepository extends JpaRepository<RegulatoryRule, UUID> {

    /** Todas las reglas activas, ordenadas por categoría y código. */
    List<RegulatoryRule> findByActiveTrueOrderByCategoryAscCodeAsc();

    /** Reglas activas de una categoría específica. */
    List<RegulatoryRule> findByCategoryAndActiveTrueOrderByCodeAsc(String category);

    /** Reglas activas sin categoría específica (aplican a todas). */
    List<RegulatoryRule> findByCategoryIsNullAndActiveTrueOrderByCodeAsc();

    /** Buscar por código (retorna la versión activa). */
    Optional<RegulatoryRule> findByCodeAndActiveTrue(String code);
}
