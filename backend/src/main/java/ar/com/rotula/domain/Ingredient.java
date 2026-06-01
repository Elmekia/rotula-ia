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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
