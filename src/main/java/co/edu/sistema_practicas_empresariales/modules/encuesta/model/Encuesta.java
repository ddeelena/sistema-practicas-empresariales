package co.edu.sistema_practicas_empresariales.modules.encuesta.model;

import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practica_encuestas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_practica_actor", columnNames = {"practica_id", "tipo_actor"})
})
@Getter
@Setter
@ToString(exclude = "practica")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practica_id", nullable = false)
    private Practica practica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actor", nullable = false, length = 50)
    private TipoActor tipoActor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EstadoEncuesta estado = EstadoEncuesta.PENDIENTE;

    @Column(name = "respuestas_json", columnDefinition = "TEXT")
    private String respuestasJson;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

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

    public enum TipoActor {
        ESTUDIANTE,
        TUTOR_EMPRESARIAL
    }

    public enum EstadoEncuesta {
        PENDIENTE,
        BORRADOR,
        COMPLETADA
    }
}
