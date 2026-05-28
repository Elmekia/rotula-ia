package ar.com.rotula.service.allergen;

import ar.com.rotula.domain.AllergenGroup;

import java.util.Set;

/**
 * Resultado del análisis de alérgenos para un producto.
 *
 * @param presentGroups         Grupos de alérgenos presentes en los ingredientes
 * @param crossContaminationGroups Grupos de alérgenos por contaminación cruzada
 * @param declarationText       Texto de declaración listo para el rótulo
 */
public record AllergenDeclarationResult(
        Set<AllergenGroup> presentGroups,
        Set<AllergenGroup> crossContaminationGroups,
        String declarationText
) {}
