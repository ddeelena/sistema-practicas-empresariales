package co.edu.sistema_practicas_empresariales.modules.practica.dto;

import co.edu.sistema_practicas_empresariales.modules.practica.enums.EstadoAvance;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AvanceDto {
    private Long          id;
    private Long          practica_id;
    private Long          corteId;
    private Integer       corteNumero;
    private String        titulo;
    private String        descripcion;
    private String        archivoUrl;
    private LocalDateTime archivoFechaCarga;
    private String        comentarioDocente;
    private EstadoAvance estado;
    private LocalDateTime createdAt;
}
