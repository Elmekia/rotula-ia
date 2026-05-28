package ar.com.rotula.service;

import java.util.List;
import java.util.Locale;

/**
 * Detecta alérgenos de declaración obligatoria según el Anexo de la
 * Resolución Conjunta 1/2023 (Res. 109/2023) del Ministerio de Salud /
 * Secretaría de Agricultura de la República Argentina.
 *
 * Los alérgenos cubiertos son los 14 grupos del anexo:
 *   1. Cereales con gluten
 *   2. Crustáceos
 *   3. Huevo
 *   4. Pescado
 *   5. Maní / cacahuate
 *   6. Soja
 *   7. Leche (incluye lactosa)
 *   8. Frutos de cáscara (nueces, almendras, avellanas, etc.)
 *   9. Apio
 *  10. Mostaza
 *  11. Sésamo / ajonjolí
 *  12. Dióxido de azufre y sulfitos
 *  13. Altramuces / lupino
 *  14. Moluscos
 */
public final class AllergenDetector {

    private AllergenDetector() {}

    /**
     * Palabras clave en minúsculas. Se busca si alguna de ellas está contenida
     * en el nombre del ingrediente (también en minúsculas).
     */
    private static final List<String> KEYWORDS = List.of(
            // 1 – Cereales con gluten
            "trigo", "centeno", "cebada", "avena", "espelta", "kamut", "farro",
            "triticale", "gluten", "harina de trigo", "almidón de trigo",
            "sémola", "cuscús", "bulgur",

            // 2 – Crustáceos
            "crustáceo", "crustaceo", "camarón", "camaron", "langosta",
            "langostino", "cangrejo", "krill", "cigala", "bogavante",

            // 3 – Huevo
            "huevo", "albumina", "ovoalbúmina", "ovoalbumina", "lisozima",
            "mayonesa", "yema", "clara de huevo",

            // 4 – Pescado
            "pescado", "anchoa", "anchoíta", "anchoita", "bacalao", "merluza",
            "atún", "atun", "salmón", "salmon", "sardina", "tilapia",
            "surimi", "trucha", "lenguado", "pejerrey", "dorado",

            // 5 – Maní / cacahuate
            "maní", "mani", "cacahuate", "cacahuete", "mantequilla de maní",

            // 6 – Soja / soya
            "soja", "soya", "lecitina de soja", "proteína de soja",
            "harina de soja", "tofu",

            // 7 – Leche y derivados
            "leche", "lactosa", "suero de leche", "caseína", "caseina",
            "lactoalbúmina", "lactoalbumina", "lactoglobulina", "manteca",
            "mantequilla", "queso", "crema", "yogur", "yogurt", "ricotta",
            "requesón", "requeson", "nata",

            // 8 – Frutos de cáscara
            "nuez", "almendra", "avellana", "anacardo", "cajú", "caju",
            "pistacho", "nuez de brasil", "nuez de macadamia",
            "nuez de pecán", "nuez de pecan", "pecán", "pecan",
            "nuez de india", "nuez moscada",

            // 9 – Apio
            "apio",

            // 10 – Mostaza
            "mostaza",

            // 11 – Sésamo / ajonjolí
            "sésamo", "sesamo", "ajonjolí", "ajonjoli", "tahini", "tahín", "tahin",

            // 12 – Dióxido de azufre y sulfitos (E220-E228)
            "dióxido de azufre", "dioxido de azufre",
            "sulfito", "bisulfito", "metabisulfito",

            // 13 – Altramuces / lupino
            "altramuz", "lupino", "lupini", "altramuces",

            // 14 – Moluscos
            "molusco", "almeja", "mejillón", "mejillon", "ostra",
            "calamar", "pulpo", "caracol", "berberecho", "vieira"
    );

    /**
     * Retorna {@code true} si el nombre del ingrediente contiene alguna de las
     * palabras clave de alérgenos de la Res. 109/2023.
     */
    public static boolean isAllergen(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) return false;
        String lower = ingredientName.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(lower::contains);
    }
}
