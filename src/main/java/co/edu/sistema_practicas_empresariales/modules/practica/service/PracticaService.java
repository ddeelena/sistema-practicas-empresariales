package co.edu.sistema_practicas_empresariales.modules.practica.service;

import co.edu.sistema_practicas_empresariales.modules.practica.dto.ChecklistDto;
import co.edu.sistema_practicas_empresariales.modules.practica.dto.PracticaDetalleDto;
import co.edu.sistema_practicas_empresariales.modules.practica.dto.PracticaResumenDto;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import co.edu.sistema_practicas_empresariales.modules.practica.request.FechaSustentacionRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.request.NotaFinalRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PracticaService {
    List<PracticaResumenDto> listarTodas();
    List<PracticaResumenDto> listarPorEstado(EstadoPracticaTipo estado);
    List<PracticaResumenDto> listarPorDocente(Long docenteId);
    PracticaDetalleDto obtenerDetalle(Long id);
    PracticaDetalleDto obtenerPracticaActivaEstudiante(Long estudianteId);
    PracticaDetalleDto crearPracticaAutomatica(Long estudianteId, Long catalogoId);
    PracticaDetalleDto registrarConvenio(Long practicaId);
    PracticaDetalleDto activarPractica(Long practicaId);
    PracticaDetalleDto cancelar(Long practicaId, String motivo);
    PracticaDetalleDto asignarDocente(Long practicaId, Long docenteId);
    PracticaDetalleDto asignarEmpresa(Long practicaId, Long empresaId);
    PracticaDetalleDto asignarTutor(Long practicaId, Long tutorId);
    PracticaDetalleDto registrarFechaSustentacion(Long practicaId, FechaSustentacionRequest req);
    PracticaDetalleDto subirDocumento(Long practicaId, String categoria, MultipartFile archivo, String emailUsuario);
    PracticaDetalleDto registrarNotaFinal(Long practicaId, NotaFinalRequest req);
    List<ChecklistDto> obtenerChecklist(Long practicaId);
    boolean tienePazYSalvo(Long practicaId);
    PracticaDetalleDto iniciarVinculacion(Long practicaId);
}
