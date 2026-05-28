package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 300)
    private String name;

    /** Peso del ingrediente en gramos ingresado por el usuario. */
    @Column(name = "weight_grams", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightGrams;

    // Field named "allergen" to produce getter isAllergen(); maps to DB column is_allergen
    @Column(name = "is_allergen", nullable = false)
    private boolean allergen;

    // ── Campos nutricionales por 100 g del ingrediente (todos opcionales) ─────

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
