package ar.com.rotula.service.nutrition;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Determina qué sellos de advertencia de exceso corresponden a un producto
 * según la Ley 27.642 y el Decreto 151/2022 (reglamentación de Argentina).
 *
 * <p>Los umbrales se evalúan sobre los valores <em>crudos</em> (sin redondeo)
 * por 100 g o 100 mL del producto formulado.
 *
 * <h3>Umbrales por tipo de producto (Decreto 151/2022, Anexo)</h3>
 * <pre>
 * Sello                      Sólidos (> por 100 g)   Bebidas (> por 100 mL)
 * ─────────────────────────  ─────────────────────   ──────────────────────
 * EXCESO EN GRASAS SAT.      6 g                     3 g
 * EXCESO EN GRASAS TRANS     2 g                     0.75 g
 * EXCESO EN SODIO            400 mg                  100 mg
 * EXCESO EN AZÚCARES         22.5 g                  6 g
 * EXCESO EN CALORÍAS         450 kcal                70 kcal
 * </pre>
 *
 * <p>Se considera producto <em>líquido</em> cuando la unidad de peso del producto
 * es "l", "ml" o "cc"; de lo contrario se trata como sólido.
 */
@Service
public class SealEvaluatorService {

    // ── Umbrales sólidos (por 100 g) ─────────────────────────────────────────
    private static final double SOLID_FAT_SAT_G    = 6.0;
    private static final double SOLID_FAT_TRANS_G  = 2.0;
    private static final double SOLID_SODIUM_MG    = 400.0;
    private static final double SOLID_SUGARS_G     = 22.5;
    private static final double SOLID_ENERGY_KCAL  = 450.0;

    // ── Umbrales bebidas (por 100 mL) ────────────────────────────────────────
    private static final double LIQUID_FAT_SAT_G   = 3.0;
    private static final double LIQUID_FAT_TRANS_G = 0.75;
    private static final double LIQUID_SODIUM_MG   = 100.0;
    private static final double LIQUID_SUGARS_G    = 6.0;
    private static final double LIQUID_ENERGY_KCAL = 70.0;

    // ── Etiquetas de sellos ───────────────────────────────────────────────────
    public static final String SEAL_GRASAS_SAT    = "EXCESO EN GRASAS SATURADAS";
    public static final String SEAL_GRASAS_TRANS  = "EXCESO EN GRASAS TRANS";
    public static final String SEAL_SODIO         = "EXCESO EN SODIO";
    public static final String SEAL_AZUCARES      = "EXCESO EN AZÚCARES";
    public static final String SEAL_CALORIAS      = "EXCESO EN CALORÍAS";

    /**
     * Evalúa los sellos de advertencia para los valores nutricionales dados.
     *
     * @param raw100g  valores crudos (sin redondear) por 100 g o 100 mL
     * @param isLiquid {@code true} si el producto es una bebida (unidad l/ml/cc)
     * @return conjunto ordenado de etiquetas de sellos que corresponden
     */
    public Set<String> evaluate(NutritionValues raw100g, boolean isLiquid) {
        Set<String> seals = new LinkedHashSet<>();

        double fatSat   = raw100g.fatSatG();
        double fatTrans = raw100g.fatTransG();
        double sodium   = raw100g.sodiumMg();
        double sugars   = raw100g.sugarsG();
        double energy   = raw100g.energyKcal();

        if (isLiquid) {
            if (fatSat   > LIQUID_FAT_SAT_G)   seals.add(SEAL_GRASAS_SAT);
            if (fatTrans > LIQUID_FAT_TRANS_G)  seals.add(SEAL_GRASAS_TRANS);
            if (sodium   > LIQUID_SODIUM_MG)    seals.add(SEAL_SODIO);
            if (sugars   > LIQUID_SUGARS_G)     seals.add(SEAL_AZUCARES);
            if (energy   > LIQUID_ENERGY_KCAL)  seals.add(SEAL_CALORIAS);
        } else {
            if (fatSat   > SOLID_FAT_SAT_G)    seals.add(SEAL_GRASAS_SAT);
            if (fatTrans > SOLID_FAT_TRANS_G)   seals.add(SEAL_GRASAS_TRANS);
            if (sodium   > SOLID_SODIUM_MG)     seals.add(SEAL_SODIO);
            if (sugars   > SOLID_SUGARS_G)      seals.add(SEAL_AZUCARES);
            if (energy   > SOLID_ENERGY_KCAL)   seals.add(SEAL_CALORIAS);
        }

        return seals;
    }
}
