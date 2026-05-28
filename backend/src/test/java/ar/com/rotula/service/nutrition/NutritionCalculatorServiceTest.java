package ar.com.rotula.service.nutrition;

import ar.com.rotula.domain.Ingredient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class NutritionCalculatorServiceTest {

    private final NutritionCalculatorService service = new NutritionCalculatorService();

    // ── Ingrediente helper ────────────────────────────────────────────────────

    private Ingredient ingredient(double weightG,
                                  double kcalPer100, double protPer100, double carbPer100,
                                  double sugPer100,  double fatPer100,  double satPer100,
                                  double transPer100,double naPer100) {
        Ingredient i = new Ingredient();
        i.setId(UUID.randomUUID());
        i.setWeightGrams(BigDecimal.valueOf(weightG));
        i.setEnergyKcalPer100g(BigDecimal.valueOf(kcalPer100));
        i.setProteinsPer100g(BigDecimal.valueOf(protPer100));
        i.setCarbsPer100g(BigDecimal.valueOf(carbPer100));
        i.setSugarsPer100g(BigDecimal.valueOf(sugPer100));
        i.setFatTotalPer100g(BigDecimal.valueOf(fatPer100));
        i.setFatSatPer100g(BigDecimal.valueOf(satPer100));
        i.setFatTransPer100g(BigDecimal.valueOf(transPer100));
        i.setSodiumMgPer100g(BigDecimal.valueOf(naPer100));
        return i;
    }

    // ── Sin datos nutricionales ───────────────────────────────────────────────

    @Test
    void calculate_retorna_null_cuando_ningun_ingrediente_tiene_datos() {
        Ingredient sinDatos = new Ingredient();
        sinDatos.setWeightGrams(BigDecimal.valueOf(100));
        // energyKcalPer100g == null

        NutritionCalculationResult result = service.calculate(
                List.of(sinDatos),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30)
        );

        assertThat(result).isNull();
    }

    @Test
    void calculate_retorna_null_cuando_lista_vacia() {
        NutritionCalculationResult result = service.calculate(
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.valueOf(30)
        );
        assertThat(result).isNull();
    }

    // ── Producto simple con un ingrediente ────────────────────────────────────

    @Test
    void calculate_un_ingrediente_todo_el_producto_es_ese_ingrediente() {
        // Harina de trigo integral, 100 g en el producto, porción 30 g
        // Como es el único ingrediente, per100g == per ingrediente/100g
        Ingredient harina = ingredient(
                100,   // 100 g en el producto
                340,   // 340 kcal/100g
                12,    // 12 g proteína
                70,    // 70 g carbs
                2,     // 2 g azúcares
                2.0,   // 2 g grasa total
                0.4,   // 0.4 g grasa sat
                0,     // 0 trans
                2      // 2 mg sodio
        );

        NutritionCalculationResult r = service.calculate(
                List.of(harina),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30)
        );

        assertThat(r).isNotNull();
        assertThat(r.servingSizeG()).isEqualByComparingTo(BigDecimal.valueOf(30));

        // Por 100 g del producto = mismo que por 100 g del ingrediente
        assertThat(r.rawPer100g().energyKcal()).isCloseTo(340.0, within(0.01));
        assertThat(r.rawPer100g().proteinsG()).isCloseTo(12.0, within(0.01));
        assertThat(r.rawPer100g().carbsG()).isCloseTo(70.0, within(0.01));
        assertThat(r.rawPer100g().sodiumMg()).isCloseTo(2.0, within(0.01));

        // Por porción (30 g) = 30% del per100g
        assertThat(r.rawPer100g().energyKcal() * 0.30).isCloseTo(102.0, within(0.1));

        // Redondeado por porción: 340 * 0.3 = 102 → > 50 → round to 100
        assertThat(r.perPortion().energyKcal()).isEqualTo(100);
        // Proteínas: 12 * 0.3 = 3.6 → round to 4
        assertThat(r.perPortion().proteinsG()).isEqualTo(4);
        // Sodio: 2 * 0.3 = 0.6 mg → < 5 → 0
        assertThat(r.perPortion().sodiumMg()).isEqualTo(0);
    }

    // ── Producto con múltiples ingredientes ───────────────────────────────────

    @Test
    void calculate_mezcla_dos_ingredientes_pesos_distintos() {
        // Harina de trigo: 200 g, kcal=360, prot=10, carbs=72, sug=1, fat=1.5, sat=0.2, trans=0, na=2
        // Azúcar:           80 g, kcal=400, prot=0,  carbs=100, sug=100, fat=0, sat=0, trans=0, na=1
        // Porción: 30 g, Total: 280 g

        Ingredient harina = ingredient(200, 360, 10, 72, 1, 1.5, 0.2, 0, 2);
        Ingredient azucar = ingredient(80, 400, 0, 100, 100, 0, 0, 0, 1);
        BigDecimal total = BigDecimal.valueOf(280);
        BigDecimal porcion = BigDecimal.valueOf(30);

        NutritionCalculationResult r = service.calculate(List.of(harina, azucar), total, porcion);

        assertThat(r).isNotNull();

        /*
         * Contribuciones totales (no por 100g):
         * kcal: 200*3.6 + 80*4.0 = 720 + 320 = 1040
         * prot: 200*0.1 + 80*0 = 20
         * carbs: 200*0.72 + 80*1.0 = 144 + 80 = 224
         * sug: 200*0.01 + 80*1.0 = 2 + 80 = 82
         * fat: 200*0.015 + 80*0 = 3
         * sat: 200*0.002 + 80*0 = 0.4
         * na: 200*0.02 + 80*0.01 = 4 + 0.8 = 4.8
         *
         * Per 100g = total / 280 * 100:
         * kcal: 1040/280*100 = 371.4
         * prot: 20/280*100 = 7.14
         * carbs: 224/280*100 = 80.0
         * sug: 82/280*100 = 29.3
         * fat: 3/280*100 = 1.07
         * sat: 0.4/280*100 = 0.14
         * na: 4.8/280*100 = 1.71
         */
        assertThat(r.rawPer100g().energyKcal()).isCloseTo(371.4, within(0.5));
        assertThat(r.rawPer100g().proteinsG()).isCloseTo(7.14, within(0.1));
        assertThat(r.rawPer100g().carbsG()).isCloseTo(80.0, within(0.1));
        assertThat(r.rawPer100g().sugarsG()).isCloseTo(29.3, within(0.1));
        assertThat(r.rawPer100g().sodiumMg()).isCloseTo(1.71, within(0.1));

        // Per100g redondeado: kcal 371.4 → > 50 → round to 10 → 370
        assertThat(r.per100g().energyKcal()).isEqualTo(370);
        // proteínas 7.14 → round to 7
        assertThat(r.per100g().proteinsG()).isEqualTo(7);
        // carbs 80.0 → round to 80
        assertThat(r.per100g().carbsG()).isEqualTo(80);
        // azúcares 29.3 → round to 29
        assertThat(r.per100g().sugarsG()).isEqualTo(29);
        // grasas sat 0.14 → < 0.5 → 0
        assertThat(r.per100g().fatSatG()).isEqualTo(0);
    }

    // ── kJ se calcula correctamente ───────────────────────────────────────────

    @Test
    void calculate_kj_es_kcal_por_factor_de_conversion() {
        Ingredient i = ingredient(100, 200, 0, 50, 50, 0, 0, 0, 0);

        NutritionCalculationResult r = service.calculate(
                List.of(i),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100)
        );

        assertThat(r).isNotNull();
        // kJ raw = 200 * 4.184 = 836.8
        assertThat(r.rawPer100g().energyKj()).isCloseTo(200 * 4.184, within(0.01));
        // kJ redondeado: 836.8 → > 210 → múltiplo de 40 → 840
        assertThat(r.per100g().energyKj()).isEqualTo(840);
    }

    // ── Ingrediente sin datos es ignorado en el cálculo ───────────────────────

    @Test
    void calculate_ingrediente_sin_datos_nutricionales_se_ignora() {
        // Solo harina tiene datos; sal no tiene
        Ingredient harina = ingredient(200, 360, 10, 72, 1, 1.5, 0.2, 0, 2);
        Ingredient sal = new Ingredient();
        sal.setWeightGrams(BigDecimal.valueOf(5));
        // energyKcalPer100g == null → ignorado

        NutritionCalculationResult r = service.calculate(
                List.of(harina, sal),
                BigDecimal.valueOf(205),
                BigDecimal.valueOf(30)
        );

        // Debe retornar resultado (harina tiene datos), ignorando la sal
        assertThat(r).isNotNull();
        // El cálculo usa total = 205 g pero solo harina aporta nutrición
        // kcal per 100g del producto = (200 * 3.60) / 205 * 100 = 720/205*100 ≈ 351.2
        assertThat(r.rawPer100g().energyKcal()).isCloseTo(351.2, within(1.0));
    }
}
