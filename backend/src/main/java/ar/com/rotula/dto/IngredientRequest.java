package ar.com.rotula.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        Boolean allergen
) {}
