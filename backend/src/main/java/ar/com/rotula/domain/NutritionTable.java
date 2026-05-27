package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nutrition_tables")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NutritionTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "serving_size_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal servingSizeG;

    @Column(name = "energy_kcal", precision = 8, scale = 2)
    private BigDecimal energyKcal;

    @Column(name = "energy_kj", precision = 8, scale = 2)
    private BigDecimal energyKj;

    @Column(name = "proteins_g", precision = 8, scale = 2)
    private BigDecimal proteinsG;

    @Column(name = "carbs_g", precision = 8, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "sugars_g", precision = 8, scale = 2)
    private BigDecimal sugarsG;

    @Column(name = "fat_total_g", precision = 8, scale = 2)
    private BigDecimal fatTotalG;

    @Column(name = "fat_saturated_g", precision = 8, scale = 2)
    private BigDecimal fatSaturatedG;

    @Column(name = "fat_trans_g", precision = 8, scale = 2)
    private BigDecimal fatTransG;

    @Column(name = "sodium_mg", precision = 8, scale = 2)
    private BigDecimal sodiumMg;

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
