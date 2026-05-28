package ar.com.rotula.domain;

/**
 * Los 14 grupos de alérgenos de declaración obligatoria según la
 * Resolución Conjunta 1/2023 (Res. 109/2023) del Ministerio de Salud
 * y la Secretaría de Agricultura de la República Argentina.
 */
public enum AllergenGroup {

    GLUTEN             ("Gluten"),
    CRUSTACEOS         ("Crustáceos"),
    HUEVO              ("Huevo"),
    PESCADO            ("Pescado"),
    MANI               ("Maní"),
    SOJA               ("Soja"),
    LECHE              ("Leche"),
    FRUTOS_DE_CASCARA  ("Frutos de cáscara"),
    APIO               ("Apio"),
    MOSTAZA            ("Mostaza"),
    SESAMO             ("Sésamo"),
    SULFITOS           ("Dióxido de azufre y sulfitos"),
    ALTRAMUCES         ("Altramuces"),
    MOLUSCOS           ("Moluscos");

    private final String label;

    AllergenGroup(String label) {
        this.label = label;
    }

    /** Etiqueta en español para mostrar en la declaración de alérgenos. */
    public String getLabel() {
        return label;
    }
}
