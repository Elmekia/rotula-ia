package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    /** Código corto de grupo (G1 … G8). */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    /** Nombre completo del grupo (TABLA I). */
    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(nullable = false)
    private boolean active;
}
