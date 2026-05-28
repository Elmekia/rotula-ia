package ar.com.rotula.service.nutrition;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.com.rotula.service.nutrition.SealEvaluatorService.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Casos de prueba con productos representativos del mercado argentino.
 * Los valores nutricionales son por 100 g para sólidos o por 100 mL para bebidas.
 */
class SealEvaluatorServiceTest {

    private final SealEvaluatorService service = new SealEvaluatorService();

    // ── Helper ────────────────────────────────────────────────────────────────

    /** fatSat, fatTrans, sodiumMg, sugarsG, energyKcal */
    private NutritionValues vals(double fatSat, double fatTrans,
                                 double sodiumMg, double sugarsG,
                                 double energyKcal) {
        return new NutritionValues(energyKcal, energyKcal * 4.184,
                0, 0, sugarsG, 0, fatSat, fatTrans, sodiumMg);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SÓLIDOS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void solido_galleta_simple_sin_sellos() {
        // Galleta de agua simple
        // fatSat=2g, trans=0.1g, sodio=250mg, azúc=5g, kcal=400
        Set<String> seals = service.evaluate(vals(2, 0.1, 250, 5, 400), false);
        assertThat(seals).isEmpty();
    }

    @Test
    void solido_galleta_rellena_tres_sellos() {
        // Galletita rellena de chocolate: alto en sat, azúcares y calorías
        // fatSat=9g, trans=0g, sodio=300mg, azúc=38g, kcal=500
        Set<String> seals = service.evaluate(vals(9, 0, 300, 38, 500), false);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_AZUCARES, SEAL_CALORIAS);
    }

    @Test
    void solido_snack_de_maiz_grasas_sat_sodio_calorias() {
        // Palitos / chizitos: fatSat=7g, trans=0, sodio=500mg, azúc=2g, kcal=480
        Set<String> seals = service.evaluate(vals(7, 0, 500, 2, 480), false);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_SODIO, SEAL_CALORIAS);
    }

    @Test
    void solido_pan_lactal_solo_sodio() {
        // Pan de molde: fatSat=1.5g, trans=0, sodio=600mg, azúc=4g, kcal=280
        Set<String> seals = service.evaluate(vals(1.5, 0, 600, 4, 280), false);
        assertThat(seals).containsExactly(SEAL_SODIO);
    }

    @Test
    void solido_papas_fritas_grasas_sat_sodio_calorias() {
        // Papas fritas de bolsa: fatSat=10g, trans=0, sodio=500mg, azúc=1g, kcal=540
        Set<String> seals = service.evaluate(vals(10, 0, 500, 1, 540), false);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_SODIO, SEAL_CALORIAS);
    }

    @Test
    void solido_manteca_todos_los_sellos() {
        // Manteca salada: fatSat=50g, trans=3g, sodio=600mg, azúc=0g, kcal=720
        Set<String> seals = service.evaluate(vals(50, 3, 600, 0, 720), false);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_GRASAS_TRANS, SEAL_SODIO, SEAL_CALORIAS);
    }

    @Test
    void solido_cacao_en_polvo_sin_sellos() {
        // Cacao amargo puro: fatSat=5.5g, trans=0, sodio=10mg, azúc=5g, kcal=350
        Set<String> seals = service.evaluate(vals(5.5, 0, 10, 5, 350), false);
        assertThat(seals).isEmpty();
    }

    @Test
    void solido_yogur_entero_sin_sellos() {
        // Yogur entero natural: fatSat=2g, trans=0, sodio=50mg, azúc=4g, kcal=60
        Set<String> seals = service.evaluate(vals(2, 0, 50, 4, 60), false);
        assertThat(seals).isEmpty();
    }

    @Test
    void solido_queso_duro_grasas_sat_y_sodio() {
        // Queso sardo: fatSat=20g, trans=1g, sodio=1000mg, azúc=0g, kcal=380
        Set<String> seals = service.evaluate(vals(20, 1, 1000, 0, 380), false);
        assertThat(seals).containsExactlyInAnyOrder(SEAL_GRASAS_SAT, SEAL_SODIO);
    }

    @Test
    void solido_fiambre_cocido_solo_sodio() {
        // Jamón cocido: fatSat=4g, trans=0, sodio=1200mg, azúc=2g, kcal=200
        Set<String> seals = service.evaluate(vals(4, 0, 1200, 2, 200), false);
        assertThat(seals).containsExactly(SEAL_SODIO);
    }

    @Test
    void solido_chocolate_negro_grasas_sat_azucares_calorias() {
        // Chocolate 70% cacao: fatSat=13g, trans=0, sodio=10mg, azúc=25g, kcal=560
        Set<String> seals = service.evaluate(vals(13, 0, 10, 25, 560), false);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_AZUCARES, SEAL_CALORIAS);
    }

    @Test
    void solido_almendras_solo_calorias() {
        // Almendras naturales: fatSat=3.7g, trans=0, sodio=1mg, azúc=4g, kcal=579
        Set<String> seals = service.evaluate(vals(3.7, 0, 1, 4, 579), false);
        assertThat(seals).containsExactly(SEAL_CALORIAS);
    }

    @Test
    void solido_arroz_blanco_sin_sellos() {
        // Arroz blanco crudo: fatSat=0.1g, trans=0, sodio=2mg, azúc=0g, kcal=360
        Set<String> seals = service.evaluate(vals(0.1, 0, 2, 0, 360), false);
        assertThat(seals).isEmpty();
    }

    @Test
    void solido_umbral_exacto_no_activa_sello() {
        // Exactamente en el umbral: > umbral activa el sello, == umbral no
        NutritionValues exactoFatSat = vals(6.0, 0, 400.0, 22.5, 450.0);
        assertThat(service.evaluate(exactoFatSat, false)).isEmpty();
    }

    @Test
    void solido_un_punto_encima_del_umbral_activa_todos_los_sellos() {
        // Ligeramente por encima de todos los umbrales de sólidos
        NutritionValues encima = vals(6.01, 2.01, 400.01, 22.51, 450.01);
        assertThat(service.evaluate(encima, false)).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_GRASAS_TRANS, SEAL_SODIO, SEAL_AZUCARES, SEAL_CALORIAS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BEBIDAS (por 100 mL)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void bebida_agua_mineral_sin_sellos() {
        Set<String> seals = service.evaluate(vals(0, 0, 5, 0, 0), true);
        assertThat(seals).isEmpty();
    }

    @Test
    void bebida_gaseosa_cola_solo_azucares() {
        // Coca-Cola: fatSat=0, trans=0, sodio=10mg, azúc=10.6g, kcal=42
        Set<String> seals = service.evaluate(vals(0, 0, 10, 10.6, 42), true);
        assertThat(seals).containsExactly(SEAL_AZUCARES);
    }

    @Test
    void bebida_jugo_de_naranja_natural_solo_azucares() {
        // Naranja exprimida: azúc=8.4g (>6), kcal=45 (<70), na=1mg
        Set<String> seals = service.evaluate(vals(0, 0, 1, 8.4, 45), true);
        assertThat(seals).containsExactly(SEAL_AZUCARES);
    }

    @Test
    void bebida_leche_entera_sin_sellos() {
        // Leche entera: fatSat=2.1g (<3), sodio=50mg (<100), azúc=4.8g (<6), kcal=61 (<70)
        Set<String> seals = service.evaluate(vals(2.1, 0.1, 50, 4.8, 61), true);
        assertThat(seals).isEmpty();
    }

    @Test
    void bebida_energizante_azucares_y_sodio() {
        // Bebida energizante: sodio=120mg (>100), azúc=12g (>6), kcal=50 (<70)
        Set<String> seals = service.evaluate(vals(0, 0, 120, 12, 50), true);
        assertThat(seals).containsExactlyInAnyOrder(SEAL_SODIO, SEAL_AZUCARES);
    }

    @Test
    void bebida_chocolate_liquido_grasas_sat_azucares_calorias() {
        // Chocolate líquido: fatSat=3.5g (>3), azúc=9g (>6), kcal=80 (>70)
        Set<String> seals = service.evaluate(vals(3.5, 0, 50, 9, 80), true);
        assertThat(seals).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_AZUCARES, SEAL_CALORIAS);
    }

    @Test
    void bebida_crema_de_leche_grasas_sat_y_calorias() {
        // Crema de leche líquida: fatSat=20g (>3), trans=0.5g (≤0.75 → sin sello), kcal=300 (>70)
        Set<String> seals = service.evaluate(vals(20, 0.5, 30, 3, 300), true);
        assertThat(seals).containsExactlyInAnyOrder(SEAL_GRASAS_SAT, SEAL_CALORIAS);
    }

    @Test
    void bebida_aceite_de_girasol_grasas_sat_y_calorias() {
        // Aceite de girasol: fatSat=12g (>3), kcal=884 (>70)
        Set<String> seals = service.evaluate(vals(12, 0.1, 0, 0, 884), true);
        assertThat(seals).containsExactlyInAnyOrder(SEAL_GRASAS_SAT, SEAL_CALORIAS);
    }

    @Test
    void bebida_salsa_de_soja_solo_sodio() {
        // Salsa de soja (shoyu): sodio=5720mg (>>100), azúc=1g, kcal=60
        Set<String> seals = service.evaluate(vals(0, 0, 5720, 1, 60), true);
        assertThat(seals).containsExactly(SEAL_SODIO);
    }

    @Test
    void bebida_umbral_exacto_no_activa_sello() {
        NutritionValues exacto = vals(3.0, 0.75, 100.0, 6.0, 70.0);
        assertThat(service.evaluate(exacto, true)).isEmpty();
    }

    @Test
    void bebida_todos_los_sellos_activados() {
        // Valores ligeramente por encima de todos los umbrales de bebidas
        NutritionValues todos = vals(3.01, 0.76, 100.01, 6.01, 70.01);
        assertThat(service.evaluate(todos, true)).containsExactlyInAnyOrder(
                SEAL_GRASAS_SAT, SEAL_GRASAS_TRANS, SEAL_SODIO, SEAL_AZUCARES, SEAL_CALORIAS);
    }

    @Test
    void bebida_gaseosa_light_sin_sellos() {
        // Gaseosa dietética: todo cero excepto mínimo sodio
        Set<String> seals = service.evaluate(vals(0, 0, 10, 0, 1), true);
        assertThat(seals).isEmpty();
    }
}
