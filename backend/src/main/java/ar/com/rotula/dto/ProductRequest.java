package ar.com.rotula.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(

        // ── Identificación interna ────────────────────────────────────────────

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
        String name,

        /**
         * Denominación del alimento según el CAA (aparece en el rótulo).
         * Ejemplo: "Galletitas dulces de avena con chips de chocolate".
         */
        @NotBlank(message = "La denominación del alimento es obligatoria")
        @Size(max = 300, message = "La denominación no puede superar los 300 caracteres")
        String denomination,

        // ── Clasificación TABLA I ─────────────────────────────────────────────

        @NotNull(message = "El grupo de alimentos es obligatorio")
        UUID foodGroupId,

        @NotNull(message = "El alimento de referencia es obligatorio")
        UUID foodItemId,

        /**
         * Porción de referencia en gramos (o mL).
         * El frontend la envía tal como viene del food_item seleccionado;
         * el backend la toma directamente de food_item para evitar manipulación.
         */
        BigDecimal servingSizeG,

        // ── Presentación ──────────────────────────────────────────────────────

        @NotNull(message = "El peso neto es obligatorio")
        @Positive(message = "El peso neto debe ser mayor a 0")
        BigDecimal netWeight,

        @NotBlank(message = "La unidad de peso es obligatoria")
        @Pattern(regexp = "^(kg|g|l|ml|u|cc)$", message = "La unidad debe ser kg, g, l, ml, u o cc")
        String weightUnit,

        // ── Registros ─────────────────────────────────────────────────────────

        @Size(max = 20, message = "El número RNE no puede superar los 20 caracteres")
        String rneNumber,

        @NotBlank(message = "El número RNPA es obligatorio")
        @Size(max = 20, message = "El número RNPA no puede superar los 20 caracteres")
        String rnpaNumber,

        // ── Contaminación cruzada ─────────────────────────────────────────────

        /**
         * Lista de grupos de alérgenos presentes en el ambiente de producción
         * (contaminación cruzada). Valores: nombres del enum AllergenGroup.
         * Ejemplo: ["MANI", "FRUTOS_DE_CASCARA"]
         */
        List<String> crossContaminationGroups,

        // ── Opciones de rótulo ────────────────────────────────────────────────

        /** Si true, se muestra el % de cada ingrediente en el rótulo. */
        boolean showIngredientPercentages,

        // ── Tabla nutricional por 100 g ───────────────────────────────────────
        // Todos opcionales; si al menos uno está presente se genera la tabla nutricional.
        // El sistema calcula por porción = valor * serving_size_g / 100.

        BigDecimal energyKcalPer100g,
        BigDecimal proteinsPer100g,
        BigDecimal carbsPer100g,
        BigDecimal sugarsPer100g,
        BigDecimal fatTotalPer100g,
        BigDecimal fatSatPer100g,
        BigDecimal fatTransPer100g,
        BigDecimal sodiumMgPer100g
) {}
