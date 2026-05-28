package ar.com.rotula.service.allergen;

import ar.com.rotula.domain.AllergenGroup;
import ar.com.rotula.service.AllergenDetector;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera la declaración de alérgenos requerida por la Resolución Conjunta
 * 1/2023 (Res. 109/2023) para el rótulo de un producto alimenticio.
 *
 * <p>Formato del texto de declaración:
 * <pre>
 *   CONTIENE: Gluten, Leche, Huevo.
 *   PUEDE CONTENER TRAZAS DE: Maní, Frutos de cáscara.
 * </pre>
 *
 * <p>Si no hay alérgenos presentes ni contaminación cruzada declarada, el
 * texto de declaración está vacío ("").
 */
@Service
public class AllergenDeclarationService {

    /**
     * Construye la declaración de alérgenos para un producto.
     *
     * @param ingredientNames      nombres de los ingredientes del producto
     * @param crossContaminationRaw valor del campo {@code crossContamination} del producto
     *                              (nombres de enum separados por coma, puede ser null)
     */
    public AllergenDeclarationResult buildDeclaration(
            List<String> ingredientNames,
            String crossContaminationRaw) {

        // Detectar alérgenos en ingredientes (orden: definición del enum)
        Set<AllergenGroup> present = EnumSet.noneOf(AllergenGroup.class);
        for (String name : ingredientNames) {
            present.addAll(AllergenDetector.detectGroups(name));
        }

        // Parsear contaminación cruzada
        Set<AllergenGroup> crossContam = parseCrossContamination(crossContaminationRaw);

        // Construir texto de declaración
        String text = buildText(present, crossContam);

        return new AllergenDeclarationResult(
                Collections.unmodifiableSet(present),
                Collections.unmodifiableSet(crossContam),
                text
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Set<AllergenGroup> parseCrossContamination(String raw) {
        if (raw == null || raw.isBlank()) return EnumSet.noneOf(AllergenGroup.class);

        Set<AllergenGroup> groups = EnumSet.noneOf(AllergenGroup.class);
        for (String token : raw.split(",")) {
            String trimmed = token.trim().toUpperCase(Locale.ROOT);
            try {
                groups.add(AllergenGroup.valueOf(trimmed));
            } catch (IllegalArgumentException ignored) {
                // token desconocido: se ignora
            }
        }
        return groups;
    }

    private static String buildText(
            Set<AllergenGroup> present,
            Set<AllergenGroup> crossContam) {

        if (present.isEmpty() && crossContam.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        if (!present.isEmpty()) {
            sb.append("CONTIENE: ");
            sb.append(labelsJoined(present));
            sb.append('.');
        }

        if (!crossContam.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append("PUEDE CONTENER TRAZAS DE: ");
            sb.append(labelsJoined(crossContam));
            sb.append('.');
        }

        return sb.toString();
    }

    private static String labelsJoined(Set<AllergenGroup> groups) {
        return groups.stream()
                .map(AllergenGroup::getLabel)
                .collect(Collectors.joining(", "));
    }
}
