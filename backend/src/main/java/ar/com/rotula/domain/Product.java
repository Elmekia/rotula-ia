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

    @Column(nullable = false, length = 300)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "net_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "weight_unit", nullable = false, length = 10)
    private String weightUnit;

    @Column(name = "rne_number", length = 20)
    private String rneNumber;

    @Column(name = "rnpa_number", length = 20)
    private String rnpaNumber;

    @Column(length = 20)
    private String status;

    /**
     * Tamaño de porción de referencia en gramos (o mL si el producto es líquido).
     * Utilizado para calcular la tabla nutricional por porción.
     * Es opcional; si no se informa, no se calcula la tabla nutricional.
     */
    @Column(name = "serving_size_g", precision = 8, scale = 2)
    private BigDecimal servingSizeG;

    /**
     * Grupos de alérgenos presentes en el ambiente de producción (contaminación cruzada).
     * Se almacena como nombres del enum {@link AllergenGroup} separados por coma.
     * Ejemplo: "MANI,FRUTOS_DE_CASCARA"
     */
    @Column(name = "cross_contamination")
    private String crossContamination;

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
