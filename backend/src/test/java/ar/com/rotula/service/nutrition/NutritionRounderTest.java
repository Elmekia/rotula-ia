package ar.com.rotula.service.nutrition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class NutritionRounderTest {

    // ── Energía en kcal ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} kcal → {1}")
    @CsvSource({
            "0.0,   0",
            "4.9,   0",     // < 5 → 0
            "5.0,   5",     // == 5 → múltiplo de 5
            "7.0,   5",     // 7 → redondea a 5 (más cercano)
            "8.0,  10",     // 8 → redondea a 10
            "23.0, 25",     // 23 → 25
            "27.0, 25",     // 27 → 25
            "28.0, 30",     // 28 → 30
            "50.0, 50",     // == 50 → 50
            "51.0, 50",     // > 50 → múltiplo de 10
            "55.0, 60",
            "65.0, 70",
            "100.0, 100",
            "443.0, 440",
            "450.0, 450",
            "475.0, 480"
    })
    void roundEnergy_kcal(double input, int expected) {
        assertThat(NutritionRounder.roundEnergy(input)).isEqualTo(expected);
    }

    // ── Energía en kJ ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} kJ → {1}")
    @CsvSource({
            "0.0,    0",
            "19.9,   0",    // < 20 → 0
            "20.0,  20",    // == 20
            "30.0,  40",    // 30 equidistante → Java round-half-up → 40
            "31.0,  40",    // 31 → más cercano a 40
            "100.0, 100",
            "210.0, 220",   // 210/20=10.5 → round-half-up → 11 → 220
            "211.0, 200",   // > 210 → múltiplo de 40
            "250.0, 240",
            "260.0, 280",
            "400.0, 400",
            "880.0, 880"
    })
    void roundEnergy_kj(double input, int expected) {
        assertThat(NutritionRounder.roundEnergyKj(input)).isEqualTo(expected);
    }

    // ── Macronutrientes en gramos ─────────────────────────────────────────────

    @ParameterizedTest(name = "{0} g → {1}")
    @CsvSource({
            "0.0,  0",
            "0.4,  0",   // < 0.5 → 0
            "0.5,  1",   // ≥ 0.5 → round to 1
            "0.7,  1",
            "1.4,  1",
            "1.5,  2",
            "3.4,  3",
            "3.5,  4",
            "9.9, 10",
            "10.0,10"
    })
    void roundGrams(double input, int expected) {
        assertThat(NutritionRounder.roundGrams(input)).isEqualTo(expected);
    }

    // ── Grasas trans ─────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} g trans → {1}")
    @CsvSource({
            "0.0,  0.0",
            "0.09, 0.0",    // < 0.1 → 0
            "0.1,  0.0",    // 0.1 rounds to 0.0 (nearest 0.5)
            "0.24, 0.0",    // 0.24 → 0.0
            "0.25, 0.5",    // 0.25 → 0.5
            "0.5,  0.5",
            "0.74, 0.5",    // 0.74 → 0.5
            "0.75, 1.0",    // 0.75 → 1.0
            "1.0,  1.0",
            "1.24, 1.0",
            "1.25, 1.5",
            "1.5,  1.5",
            "1.74, 1.5",
            "1.75, 2.0",
            "2.0,  2.0",    // == 2 → still in 0.5 range
            "2.4,  2.0",    // > 2 → round to 1 g
            "2.5,  3.0",
            "3.0,  3.0"
    })
    void roundTrans(double input, double expected) {
        assertThat(NutritionRounder.roundTrans(input)).isEqualTo(expected);
    }

    // ── Sodio en mg ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} mg → {1}")
    @CsvSource({
            "0.0,    0",
            "4.9,    0",    // < 5 → 0
            "5.0,    5",    // == 5
            "7.0,    5",
            "8.0,   10",
            "45.0,  45",
            "48.0,  50",
            "140.0,140",    // == 140 → múltiplo de 5
            "141.0,140",    // > 140 → múltiplo de 10
            "145.0,150",
            "200.0,200",
            "394.0,390",
            "396.0,400",
            "1000.0,1000"
    })
    void roundSodium(double input, int expected) {
        assertThat(NutritionRounder.roundSodium(input)).isEqualTo(expected);
    }

    // ── round() integración ──────────────────────────────────────────────────

    @Test
    void round_aplica_todas_las_reglas_en_conjunto() {
        NutritionValues raw = new NutritionValues(
                112.0,                  // kcal → > 50 → round to 110
                112.0 * 4.184,         // kJ
                2.14,                   // proteínas → ≥ 0.5 → 2
                24.0,                   // carbs → 24
                8.78,                   // azúcares → 9
                0.32,                   // grasa total → < 0.5 → 0
                0.04,                   // grasa sat → < 0.5 → 0
                0.08,                   // trans → < 0.1 → 0
                0.51                    // sodio → < 5 → 0
        );

        RoundedNutritionValues r = NutritionRounder.round(raw);

        assertThat(r.energyKcal()).isEqualTo(110);
        assertThat(r.proteinsG()).isEqualTo(2);
        assertThat(r.carbsG()).isEqualTo(24);
        assertThat(r.sugarsG()).isEqualTo(9);
        assertThat(r.fatTotalG()).isEqualTo(0);
        assertThat(r.fatSatG()).isEqualTo(0);
        assertThat(r.fatTransG()).isEqualTo(0.0);
        assertThat(r.sodiumMg()).isEqualTo(0);
    }

    @Test
    void round_producto_tipico_con_sodio_alto() {
        NutritionValues raw = new NutritionValues(
                280.0,   // kcal → round to 280
                280.0 * 4.184,
                12.0,    // proteínas → 12
                30.0,    // carbs → 30
                5.0,     // azúcares → 5
                9.0,     // grasa total → 9
                3.2,     // grasa sat → 3
                0.0,     // trans → 0
                850.0    // sodio → > 140 → round to 10 → 850
        );

        RoundedNutritionValues r = NutritionRounder.round(raw);

        assertThat(r.energyKcal()).isEqualTo(280);
        assertThat(r.proteinsG()).isEqualTo(12);
        assertThat(r.sodiumMg()).isEqualTo(850);
        assertThat(r.fatSatG()).isEqualTo(3);
    }
}
