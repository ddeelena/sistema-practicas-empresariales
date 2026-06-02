package co.edu.sistema_practicas_empresariales.modules.practica.service;

import co.edu.sistema_practicas_empresariales.modules.practica.dto.AvanceDto;
import co.edu.sistema_practicas_empresariales.modules.practica.enums.EstadoAvance;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Avance;
import co.edu.sistema_practicas_empresariales.modules.practica.request.ComentarioDocenteRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.request.CrearAvanceRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AvanceService {
    List<AvanceDto> listarPorPractica(Long practicaId);
    List<AvanceDto> listarPendientesPorDocente(Long docenteId);
    AvanceDto crearAvance(Long practicaId, CrearAvanceRequest req, MultipartFile archivo);
    AvanceDto agregarComentarioDocente(Long avanceId, ComentarioDocenteRequest req);
    AvanceDto cambiarEstado(Long avanceId, EstadoAvance nuevoEstado);
    AvanceDto toDto(Avance a);
}
