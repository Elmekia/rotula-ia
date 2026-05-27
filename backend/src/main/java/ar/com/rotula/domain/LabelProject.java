package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "label_projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabelProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "legal_denomination", length = 500)
    private String legalDenomination;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manufacturer_info", columnDefinition = "jsonb")
    private String manufacturerInfo;

    @Column(name = "disclaimer_accepted", nullable = false)
    private boolean disclaimerAccepted;

    @Column(name = "disclaimer_at")
    private OffsetDateTime disclaimerAt;

    @Column(name = "disclaimer_by")
    private UUID disclaimerBy;

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
