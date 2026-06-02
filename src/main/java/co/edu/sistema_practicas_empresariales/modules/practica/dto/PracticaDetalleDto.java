package co.edu.sistema_practicas_empresariales.modules.practica.dto;

import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPractica;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de detalle completo de una práctica.
 */
@Data
@Builder
public class PracticaDetalleDto {
    private Long           id;
    private EstadoPracticaTipo estado;

    // Catálogo
    private Long   catalogoPracticaId;
    private String nombrePractica;
    private String materiaNucleo;
    private String descripcion;
    private String programa;
    private Integer numeroPractica;

    // Participantes
    private Long   estudianteId;
    private String nombreEstudiante;
    private Long   empresaId;
    private String nombreEmpresa;
    private Long   docenteId;
    private String nombreDocente;
    private Long   tutorId;
    private String nombreTutor;
    private String cargoTutor;

    // Fechas
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaSustentacion;

    // Documentos
    private String        arlUrl;
    private LocalDateTime arlFechaCarga;
    private String        planeadorUrl;
    private LocalDateTime planeadorFechaCarga;
    private String        informeEjecutivoUrl;
    private String        presentacionUrl;
    private String        documentoFinalUrl;

    // Calificaciones
    private BigDecimal notaDocente;
    private BigDecimal notaTutor;
    private BigDecimal notaFinal;

    // Cortes y checklist
    private List<ChecklistDto> checklist;
    private Boolean   tienePazYSalvo;

    private String materiaNucleoCodigo;
    private int duracionSemanas;
    private String resultado;
}