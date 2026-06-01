package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Nombre interno del producto (no aparece en el rótulo). */
    @Column(nullable = false, length = 300)
    private String name;

    /**
     * Denominación del alimento según el CAA (aparece en el rótulo).
     * Ej.: "Galletitas dulces de avena".
     */
    @Column(length = 300)
    private String denomination;

    // ── Clasificación por TABLA I ─────────────────────────────────────────

    /**
     * Grupo de alimentos de la TABLA I (Res. Conjunta 21/2023).
     * Reemplaza al campo category — NO aparece en el rótulo.
     */
    @Column(name = "food_group_id")
    private UUID foodGroupId;

    /**
     * Alimento específico dentro del grupo.
     * Determina la porción de referencia (serving_size_g).
     */
    @Column(name = "food_item_id")
    private UUID foodItemId;

    // ── Presentación ──────────────────────────────────────────────────────

    @Column(name = "net_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "weight_unit", nullable = false, length = 10)
    private String weightUnit;

    /**
     * Tamaño de porción en gramos (o mL para líquidos).
     * Se auto-completa desde food_item.portion_grams al seleccionar el alimento.
     * No es editable por el usuario.
     */
    @Column(name = "serving_size_g", precision = 8, scale = 2)
    private BigDecimal servingSizeG;

    // ── Registros ─────────────────────────────────────────────────────────

    @Column(name = "rne_number", length = 20)
    private String rneNumber;

    /** Obligatorio desde V8. Número de Registro Nacional de Producto Alimenticio. */
    @Column(name = "rnpa_number", length = 20)
    private String rnpaNumber;

    // ── Alérgenos / contaminación cruzada ─────────────────────────────────

    /**
     * Grupos de alérgenos presentes en el ambiente de producción.
     * Se almacena como nombres del enum {@link AllergenGroup} separados por coma.
     * Ejemplo: "MANI,FRUTOS_DE_CASCARA"
     */
    @Column(name = "cross_contamination")
    private String crossContamination;

    // ── Opciones de rótulo ────────────────────────────────────────────────

    /**
     * Si true, el % de cada ingrediente se muestra en la lista de ingredientes
     * del rótulo (es optativo según la normativa vigente).
     */
    @Column(name = "show_ingredient_percentages", nullable = false)
    private boolean showIngredientPercentages;

    // ── Tabla nutricional por 100 g ───────────────────────────────────────
    // El sistema calcula los valores por porción = campo * serving_size_g / 100

    @Column(name = "energy_kcal_per100g", precision = 8, scale = 2)
    private BigDecimal energyKcalPer100g;

    @Column(name = "proteins_g_per100g", precision = 8, scale = 2)
    private BigDecimal proteinsPer100g;

    @Column(name = "carbs_g_per100g", precision = 8, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(name = "sugars_g_per100g", precision = 8, scale = 2)
    private BigDecimal sugarsPer100g;

    @Column(name = "fat_total_g_per100g", precision = 8, scale = 2)
    private BigDecimal fatTotalPer100g;

    @Column(name = "fat_sat_g_per100g", precision = 8, scale = 2)
    private BigDecimal fatSatPer100g;

    @Column(name = "fat_trans_g_per100g", precision = 8, scale = 2)
    private BigDecimal fatTransPer100g;

    @Column(name = "sodium_mg_per100g", precision = 8, scale = 2)
    private BigDecimal sodiumMgPer100g;

    // ── Auditoría ─────────────────────────────────────────────────────────

    @Column(length = 20)
    private String status;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
