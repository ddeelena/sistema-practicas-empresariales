package co.edu.sistema_practicas_empresariales.modules.evaluacion.model;

import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "practica_evaluaciones")
@Getter
@Setter
@ToString(exclude = "practica")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practica_id", nullable = false, unique = true)
    private Practica practica;

    @Column(name = "nota_docente", precision = 3, scale = 2)
    private BigDecimal notaDocente;

    @Column(name = "observaciones_docente", columnDefinition = "TEXT")
    private String observacionesDocente;

    @Column(name = "fecha_evaluacion_docente")
    private LocalDateTime fechaEvaluacionDocente;

    @Column(name = "nota_tutor", precision = 3, scale = 2)
    private BigDecimal notaTutor;

    @Column(name = "observaciones_tutor", columnDefinition = "TEXT")
    private String observacionesTutor;

    @Column(name = "fecha_evaluacion_tutor")
    private LocalDateTime fechaEvaluacionTutor;

    @Column(name = "nota_final", precision = 3, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "observaciones_finales", columnDefinition = "TEXT")
    private String observacionesFinales;

    @Column(name = "fecha_evaluacion_final")
    private LocalDateTime fechaEvaluacionFinal;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
