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
        Boolean allergen,

        // ── Campos nutricionales por 100 g del ingrediente (todos opcionales) ──

        /** Energía en kcal por 100 g del ingrediente (opcional). */
        BigDecimal energyKcalPer100g,

        /** Proteínas en gramos por 100 g del ingrediente (opcional). */
        BigDecimal proteinsPer100g,

        /** Carbohidratos totales en gramos por 100 g del ingrediente (opcional). */
        BigDecimal carbsPer100g,

        /** Azúcares (fracción de carbohidratos) en gramos por 100 g (opcional). */
        BigDecimal sugarsPer100g,

        /** Grasas totales en gramos por 100 g del ingrediente (opcional). */
        BigDecimal fatTotalPer100g,

        /** Grasas saturadas en gramos por 100 g del ingrediente (opcional). */
        BigDecimal fatSatPer100g,

        /** Grasas trans en gramos por 100 g del ingrediente (opcional). */
        BigDecimal fatTransPer100g,

        /** Sodio en miligramos por 100 g del ingrediente (opcional). */
        BigDecimal sodiumMgPer100g
) {}
