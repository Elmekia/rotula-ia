package ar.com.rotula.service;

import ar.com.rotula.domain.AllergenGroup;

import java.util.*;

/**
 * Detecta alérgenos de declaración obligatoria según el Anexo de la
 * Resolución Conjunta 1/2023 (Res. 109/2023) del Ministerio de Salud /
 * Secretaría de Agricultura de la República Argentina.
 *
 * <p>Los 14 grupos cubiertos son los del {@link AllergenGroup} enum.
 * Misma lógica que {@code allergenDetector.ts} en el frontend.
 */
public final class AllergenDetector {

    private AllergenDetector() {}

    /**
     * Términos que, aunque contienen una keyword, NO son alérgenos
     * según la Res. 109/2023 (e.g. especias con nombre similar).
     */
    private static final List<String> EXCLUSIONS = List.of(
            "nuez moscada"          // especia (Myristica fragrans), no fruto de cáscara
    );

    /**
     * Palabras clave por grupo de alérgeno.
     * Se busca si alguna de las keywords está contenida en el nombre del
     * ingrediente (también en minúsculas).
     */
    private static final Map<AllergenGroup, List<String>> GROUP_KEYWORDS;

    static {
        Map<AllergenGroup, List<String>> m = new EnumMap<>(AllergenGroup.class);

        // 1 – Cereales con gluten
        m.put(AllergenGroup.GLUTEN, List.of(
                "trigo", "centeno", "cebada", "avena", "espelta", "kamut", "farro",
                "triticale", "gluten", "harina de trigo", "almidón de trigo",
                "sémola", "semola", "cuscús", "cuscus", "bulgur",
                "malta", "extracto de malta", "cebada malteada",
                "salvado de trigo", "salvado de avena", "germen de trigo",
                "proteína de trigo", "gluten de trigo", "copos de avena"
        ));

        // 2 – Crustáceos
        m.put(AllergenGroup.CRUSTACEOS, List.of(
                "crustáceo", "crustaceo", "camarón", "camaron", "langosta",
                "langostino", "cangrejo", "krill", "cigala", "bogavante",
                "gamba", "quisquilla"
        ));

        // 3 – Huevo
        m.put(AllergenGroup.HUEVO, List.of(
                "huevo", "albumina", "ovoalbúmina", "ovoalbumina", "lisozima",
                "ovomucina", "ovomucoide",
                "mayonesa", "yema", "clara de huevo", "lecitina de huevo"
        ));

        // 4 – Pescado
        m.put(AllergenGroup.PESCADO, List.of(
                "pescado", "anchoa", "anchoíta", "anchoita", "bacalao", "merluza",
                "atún", "atun", "salmón", "salmon", "sardina", "tilapia",
                "surimi", "trucha", "lenguado", "pejerrey", "dorado",
                "corvina", "boga", "pacú", "pacu", "surubí", "surubi", "bagre"
        ));

        // 5 – Maní / cacahuate
        m.put(AllergenGroup.MANI, List.of(
                "maní", "mani", "cacahuate", "cacahuete", "mantequilla de maní"
        ));

        // 6 – Soja / soya
        m.put(AllergenGroup.SOJA, List.of(
                "soja", "soya", "lecitina de soja", "proteína de soja",
                "harina de soja", "tofu", "edamame", "miso", "tempeh"
        ));

        // 7 – Leche y derivados
        m.put(AllergenGroup.LECHE, List.of(
                "leche", "lactosa", "suero de leche", "caseína", "caseina",
                "caseínato", "caseinato",
                "lactosuero", "suero lácteo",
                "lactoalbúmina", "lactoalbumina", "lactoglobulina",
                "manteca", "mantequilla", "queso", "crema", "yogur", "yogurt",
                "ricotta", "requesón", "requeson", "nata"
        ));

        // 8 – Frutos de cáscara (Res. 109/2023: almendra, avellana, nuez, anacardo,
        //     pecán, nuez de Brasil, pistacho, macadamia / Queensland)
        m.put(AllergenGroup.FRUTOS_DE_CASCARA, List.of(
                "almendra", "avellana", "anacardo", "cajú", "caju",
                "pistacho", "nuez de brasil", "nuez de macadamia",
                "nuez de pecán", "nuez de pecan", "pecán", "pecan",
                "nuez"   // nuez común (Juglans regia) — EXCLUSIONS filtra "nuez moscada"
        ));

        // 9 – Apio
        m.put(AllergenGroup.APIO, List.of("apio"));

        // 10 – Mostaza
        m.put(AllergenGroup.MOSTAZA, List.of("mostaza"));

        // 11 – Sésamo / ajonjolí
        m.put(AllergenGroup.SESAMO, List.of(
                "sésamo", "sesamo", "ajonjolí", "ajonjoli", "tahini", "tahín", "tahin"
        ));

        // 12 – Dióxido de azufre y sulfitos (E220-E228)
        m.put(AllergenGroup.SULFITOS, List.of(
                "dióxido de azufre", "dioxido de azufre",
                "sulfito", "bisulfito", "metabisulfito"
        ));

        // 13 – Altramuces / lupino
        m.put(AllergenGroup.ALTRAMUCES, List.of(
                "altramuz", "lupino", "lupini", "altramuces"
        ));

        // 14 – Moluscos
        m.put(AllergenGroup.MOLUSCOS, List.of(
                "molusco", "almeja", "mejillón", "mejillon", "ostra",
                "calamar", "pulpo", "caracol", "berberecho", "vieira",
                "chipirón", "chipiron", "jibia", "sepia"
        ));

        GROUP_KEYWORDS = Collections.unmodifiableMap(m);
    }

    /**
     * Retorna el conjunto de grupos de alérgenos detectados en el nombre
     * del ingrediente. Devuelve un conjunto vacío si no se detecta ninguno.
     *
     * <p>El orden del resultado es el definido por el enum {@link AllergenGroup}.
     */
    public static Set<AllergenGroup> detectGroups(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) return Set.of();
        String lower = ingredientName.toLowerCase(Locale.ROOT);
        if (EXCLUSIONS.stream().anyMatch(lower::contains)) return Set.of();

        Set<AllergenGroup> groups = EnumSet.noneOf(AllergenGroup.class);
        for (Map.Entry<AllergenGroup, List<String>> entry : GROUP_KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(lower::contains)) {
                groups.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(groups);
    }

    /**
     * Retorna {@code true} si el nombre del ingrediente contiene alguna de las
     * palabras clave de alérgenos de la Res. 109/2023.
     *
     * <p>Equivalente a {@code !detectGroups(ingredientName).isEmpty()}.
     */
    public static boolean isAllergen(String ingredientName) {
        return !detectGroups(ingredientName).isEmpty();
    }
}
