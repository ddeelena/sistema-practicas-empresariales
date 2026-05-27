package co.edu.sistema_practicas_empresariales.modules.usuario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    public enum Nombre {
        ADMINISTRADOR,
        DIRECCION,
        COORDINACION_ACADEMICA,
        COORDINADOR_PRACTICA,
        SECRETARIA,
        DOCENTE_ASESOR,
        EMPRESA_VINCULADA,
        TUTOR_EMPRESARIAL,
        ESTUDIANTE
    }
}
