package co.edu.sistema_practicas_empresariales.modules.practica.dto;

import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPractica;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;

/**
 * DTO de resumen para listados de prácticas.
 */
@Data
@Builder
public class PracticaResumenDto {
    private Long          id;
    private String        nombrePractica;
    private String        materiaNucleo;
    private String        programa;
    private String        nombreEstudiante;
    private Long          estudianteId;
    private String        nombreEmpresa;
    private Long          empresaId;
    private String        nombreDocente;
    private Long          docenteId;
    private EstadoPracticaTipo estado;
    private LocalDate fechaInicio;
    private LocalDate     fechaFin;
    private Boolean       tienePazYSalvo;
}