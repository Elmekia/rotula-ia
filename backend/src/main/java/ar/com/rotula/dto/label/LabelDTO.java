package ar.com.rotula.dto.label;

import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rótulo completo estructurado de un producto, listo para renderizar y exportar.
 *
 * <p>Este DTO es el "snapshot" generado por {@code POST /labels/generate} y
 * almacenado como JSON en {@code label_projects.label_data}. Incluye todos
 * los elementos normativos del rótulo según el CAA:
 * <ul>
 *   <li>Denominación legal de venta</li>
 *   <li>Lista de ingredientes ordenada por peso decreciente</li>
 *   <li>Tabla nutricional (CAA Art. 1354)</li>
 *   <li>Sellos de advertencia (Ley 27.642 / Decreto 151/2022)</li>
 *   <li>Declaración de alérgenos (Res. 109/2023)</li>
 *   <li>Claims nutricionales aplicables (CAA Art. 1385)</li>
 * </ul>
 *
 * @param productId        ID del producto de origen.
 * @param productName      Nombre comercial del producto.
 * @param legalDenomination Denominación legal sugerida según CAA (puede ser null).
 * @param category         Denominación del alimento (antes: categoría).
 * @param netWeight        Peso / volumen neto.
 * @param weightUnit       Unidad del peso neto.
 * @param rneNumber        Número RNE (puede ser null).
 * @param rnpaNumber       Número RNPA (puede ser null).
 * @param ingredients      Ingredientes en orden decreciente de peso.
 * @param nutrition        Tabla nutricional calculada (null si faltan datos).
 * @param seals            Sellos de advertencia que corresponden.
 * @param allergens        Declaración de alérgenos formateada.
 * @param claims           Claims nutricionales aplicables (ej. "Bajo en sodio").
 * @param legalNameAlert   {@code true} si la denominación comercial difiere de la legal.
 */
public record LabelDTO(
        UUID productId,
        String productName,
        String legalDenomination,
        String category,
        BigDecimal netWeight,
        String weightUnit,
        String rneNumber,
        String rnpaNumber,
        List<IngredientItem> ingredients,
        NutritionCalculationResult nutrition,
        Set<String> seals,
        AllergenDeclarationResult allergens,
        List<String> claims,
        boolean legalNameAlert
) {}
