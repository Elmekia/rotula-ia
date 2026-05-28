package ar.com.rotula.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record IngredientRequest(

        @NotBlank(message = "El nombre del ingrediente es obligatorio")
        @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
        String name,

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.001", message = "El porcentaje debe ser mayor a 0")
        @DecimalMax(value = "100.000", message = "El porcentaje no puede superar el 100%")
        BigDecimal percentage,

        /**
         * Si es null, el backend aplica detección automática según Res. 109/2023.
         * Si se envía explícitamente true/false, se usa ese valor.
         */
        Boolean allergen,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 0, message = "El orden debe ser mayor o igual a 0")
        Integer sortOrder
) {}
