package ar.com.rotula.service.nutrition;

import ar.com.rotula.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para {@link NutritionCalculatorService}.
 *
 * La información nutricional ahora vive en el producto (por 100 g);
 * el servicio escala a por-porción usando product.serving_size_g.
 */
class NutritionCalculatorServiceTest {

    private final NutritionCalculatorService service = new NutritionCalculatorService();

    // ── helper ────────────────────────────────────────────────────────────────

    private Product product(
            double servingSizeG,
            double kcal, double prot, double carbs, double sugars,
            double fat,  double sat,  double trans,  double sodium) {
        return Product.builder()
                .servingSizeG(BigDecimal.valueOf(servingSizeG))
                .energyKcalPer100g(BigDecimal.valueOf(kcal))
                .proteinsPer100g(BigDecimal.valueOf(prot))
                .carbsPer100g(BigDecimal.valueOf(carbs))
                .sugarsPer100g(BigDecimal.valueOf(sugars))
                .fatTotalPer100g(BigDecimal.valueOf(fat))
                .fatSatPer100g(BigDecimal.valueOf(sat))
                .fatTransPer100g(BigDecimal.valueOf(trans))
                .sodiumMgPer100g(BigDecimal.valueOf(sodium))
                .weightUnit("g")
                .netWeight(BigDecimal.valueOf(200))
                .build();
    }

    // ── Sin datos nutricionales ───────────────────────────────────────────────

    @Test
    void calculate_retorna_null_cuando_no_hay_datos_nutricionales() {
        Product p = Product.builder()
                .servingSizeG(BigDecimal.valueOf(30))
                .weightUnit("g")
                .netWeight(BigDecimal.valueOf(100))
                .build();
        // todos los campos nutricionales son null

        assertThat(service.calculate(p)).isNull();
    }

    @Test
    void calculate_retorna_null_cuando_no_hay_porcion_definida() {
        Product p = Product.builder()
                .energyKcalPer100g(BigDecimal.valueOf(360))
                .weightUnit("g")
                .netWeight(BigDecimal.valueOf(100))
                .build();
        // servingSizeG es null

        assertThat(service.calculate(p)).isNull();
    }

    // ── Cálculo básico: escala por 100g → por porción ────────────────────────

    @Test
    void calculate_porcion_30g_escala_correctamente() {
        // Galletitas: 360 kcal/100g, porción 30g
        // Esperado por porción: 360 * 30 / 100 = 108 kcal
        Product p = product(30, 360, 10, 72, 2, 1.5, 0.4, 0, 3);

        NutritionCalculationResult r = service.calculate(p);

        assertThat(r).isNotNull();
        assertThat(r.servingSizeG()).isEqualByComparingTo(BigDecimal.valueOf(30));

        // Per 100g: mismo que los campos del producto
        assertThat(r.rawPer100g().energyKcal()).isCloseTo(360.0, within(0.01));
        assertThat(r.rawPer100g().proteinsG()).isCloseTo(10.0, within(0.01));
        assertThat(r.rawPer100g().carbsG()).isCloseTo(72.0, within(0.01));
        assertThat(r.rawPer100g().sodiumMg()).isCloseTo(3.0, within(0.01));

        // Por porción (30g) = campo * 30 / 100
        assertThat(r.rawPer100g().energyKcal() * 0.30).isCloseTo(108.0, within(0.1));
    }

    @Test
    void calculate_porcion_200ml_liquido() {
        // Jugo: 45 kcal/100mL, porción 200mL → 90 kcal por porción
        Product p = product(200, 45, 0, 10, 10, 0, 0, 0, 10);

        NutritionCalculationResult r = service.calculate(p);

        assertThat(r).isNotNull();
        // Por porción: 45 * 200 / 100 = 90 kcal (raw)
        assertThat(r.rawPer100g().energyKcal() * 2.0).isCloseTo(90.0, within(0.1));
    }

    // ── kJ se calcula correctamente ───────────────────────────────────────────

    @Test
    void calculate_kj_es_kcal_por_factor_de_conversion() {
        Product p = product(100, 200, 0, 50, 50, 0, 0, 0, 0);

        NutritionCalculationResult r = service.calculate(p);

        assertThat(r).isNotNull();
        // kJ raw = 200 * 4.184 = 836.8
        assertThat(r.rawPer100g().energyKj()).isCloseTo(200 * 4.184, within(0.01));
        // kJ redondeado: 836.8 → > 210 → múltiplo de 40 → 840
        assertThat(r.per100g().energyKj()).isEqualTo(840);
    }

    // ── Redondeo ─────────────────────────────────────────────────────────────

    @Test
    void calculate_redondeo_kcal_por_porcion() {
        // 340 kcal/100g, porción 30g → 102 kcal por porción → > 50 → round to 100
        Product p = product(30, 340, 12, 70, 2, 2, 0.4, 0, 2);

        NutritionCalculationResult r = service.calculate(p);

        assertThat(r).isNotNull();
        assertThat(r.perPortion().energyKcal()).isEqualTo(100);
    }

    @Test
    void calculate_sodio_bajo_redondeado_a_cero() {
        // 2 mg sodio/100g, porción 30g → 0.6 mg → < 5 → redondeado a 0
        Product p = product(30, 340, 12, 70, 2, 2, 0.4, 0, 2);

        NutritionCalculationResult r = service.calculate(p);

        assertThat(r).isNotNull();
        assertThat(r.perPortion().sodiumMg()).isEqualTo(0);
    }
}
