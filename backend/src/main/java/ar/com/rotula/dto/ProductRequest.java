package ar.com.rotula.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
        String name,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
        String category,

        @NotNull(message = "El peso neto es obligatorio")
        @Positive(message = "El peso neto debe ser mayor a 0")
        BigDecimal netWeight,

        @NotBlank(message = "La unidad de peso es obligatoria")
        @Pattern(regexp = "^(kg|g|l|ml|u|cc)$", message = "La unidad debe ser kg, g, l, ml, u o cc")
        String weightUnit,

        @Size(max = 20, message = "El número RNE no puede superar los 20 caracteres")
        String rneNumber,

        @Size(max = 20, message = "El número RNPA no puede superar los 20 caracteres")
        String rnpaNumber,

        /**
         * Tamaño de porción en gramos (o mL para líquidos).
         * Requerido para calcular la tabla nutricional por porción.
         * Si no se informa, la tabla nutricional no se calcula.
         */
        @Positive(message = "El tamaño de porción debe ser mayor a 0")
        BigDecimal servingSizeG,

        /**
         * Grupos de alérgenos presentes por contaminación cruzada.
         * Nombres del enum AllergenGroup separados por coma.
         * Ejemplo: "MANI,FRUTOS_DE_CASCARA"
         */
        String crossContamination
) {}
