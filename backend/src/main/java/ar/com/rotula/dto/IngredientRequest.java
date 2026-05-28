package ar.com.rotula.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record IngredientRequest(

        @NotBlank(message = "El nombre del ingrediente es obligatorio")
        @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
        String name,

        /** Peso del ingrediente en gramos. El % se calcula automáticamente sobre el total del producto. */
        @NotNull(message = "El peso es obligatorio")
        @DecimalMin(value = "0.001", message = "El peso debe ser mayor a 0")
        BigDecimal weightGrams,

        /**
         * Si es null, el backend aplica detección automática según Res. 109/2023.
         * Si se envía explícitamente true/false, se usa ese valor.
         */
        Boolean allergen
) {}
