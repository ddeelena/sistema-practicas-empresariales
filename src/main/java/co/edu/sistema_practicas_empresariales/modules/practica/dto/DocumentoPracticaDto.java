package co.edu.sistema_practicas_empresariales.modules.practica.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoPracticaDto {
    private Long          id;
    private String        nombre;
    private String        url;
    private String        categoria;
    private LocalDateTime fechaCarga;
    private String        cargadoPorEmail;
    private String        estado;
}
