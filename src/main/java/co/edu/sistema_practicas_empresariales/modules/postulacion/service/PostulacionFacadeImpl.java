package co.edu.sistema_practicas_empresariales.modules.postulacion.service;

import co.edu.sistema_practicas_empresariales.modules.postulacion.dto.PostulacionRequestDto;
import co.edu.sistema_practicas_empresariales.modules.postulacion.dto.PostulacionResponseDto;
import co.edu.sistema_practicas_empresariales.modules.postulacion.model.EstadoPostulacionTipo;
import co.edu.sistema_practicas_empresariales.modules.postulacion.model.Postulacion;
import co.edu.sistema_practicas_empresariales.modules.postulacion.repository.PostulacionRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.Estudiante;
import co.edu.sistema_practicas_empresariales.modules.estudiante.repository.EstudianteRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import co.edu.sistema_practicas_empresariales.modules.vacante.model.Vacante;
import co.edu.sistema_practicas_empresariales.modules.vacante.repository.VacanteRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.PracticaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;

/**
 * Fachada (Facade) que orquesta múltiples servicios para cumplir casos de uso complejos de Postulacion.
 * Actúa como intermediario entre los controladores y los servicios subyacentes, ocultando la complejidad del sistema.
 * 
 * <p>Esta clase ha sido documentada para proveer una comprensión clara
 * de su responsabilidad dentro de la arquitectura del sistema de prácticas empresariales.</p>
 * 
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostulacionFacadeImpl implements PostulacionFacade {

    private final PostulacionRepository postulacionRepository;
    private final VacanteRepository     vacanteRepository;
    private final EstudianteRepository  estudianteRepository;
    private final PracticaRepository    practicaRepository;

    // ── Crear postulación ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostulacionResponseDto crearPostulacion(PostulacionRequestDto dto) {
        Objects.requireNonNull(dto, "PostulacionRequestDto no puede ser null");

        Vacante vacante = vacanteRepository.findById(dto.getVacanteId())
                .orElseThrow(() -> new IllegalArgumentException("Vacante no encontrada"));

        Estudiante estudiante = estudianteRepository.findById(dto.getEstudianteId())
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        if (postulacionRepository.findByVacanteIdAndEstudianteId(vacante.getId(), estudiante.getId()).isPresent()) {
            throw new IllegalStateException("El estudiante ya se ha postulado a esta vacante");
        }

        Postulacion postulacion = Postulacion.builder()
                .vacante(vacante)
                .estudiante(estudiante)
                .observaciones(dto.getObservaciones())
                .estado(EstadoPostulacionTipo.POSTULADO)
                .build();

        return mapToResponse(postulacionRepository.save(postulacion));
    }

    // ── Actualizar estado ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostulacionResponseDto actualizarEstado(Long id, String estado) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Postulacion no encontrada"));

        EstadoPostulacionTipo nuevoEstado = parsearEstado(estado);

        validarTransicion(postulacion.getEstado(), nuevoEstado);
        postulacion.setEstado(nuevoEstado);
        postulacion = postulacionRepository.save(postulacion);

        if (nuevoEstado == EstadoPostulacionTipo.SELECCIONADO) {
            asignarEmpresaAPractica(postulacion);
        }

        return mapToResponse(postulacion);
    }

    // ── Lógica de negocio al seleccionar ──────────────────────────────────────

    /**
     * Cuando un estudiante es SELECCIONADO, asigna automáticamente la empresa
     * de la vacante a su práctica activa (si aún no tiene empresa asignada).
     */
    private void asignarEmpresaAPractica(Postulacion postulacion) {
        Long estudianteId = postulacion.getEstudiante().getId();
        Long empresaId    = postulacion.getVacante().getEmpresa().getId();

        practicaRepository.findPracticaActivaByEstudiante(estudianteId).ifPresentOrElse(
                practica -> {
                    if (practica.getEmpresaId() != null) {
                        log.info("Estudiante {} ya tiene empresa {} — no se sobreescribe",
                                estudianteId, practica.getEmpresaId());
                        return;
                    }
                    practica.setEmpresaId(empresaId);

                    // Transicionar estado si está en ASIGNADA_PENDIENTE_INICIO
                    if (practica.getEstado() == EstadoPracticaTipo.ASIGNADA_PENDIENTE_INICIO) {
                        practica.iniciarVinculacion(); // → EN_PROCESO_VINCULACION
                    }

                    practicaRepository.save(practica);
                    log.info("Empresa {} asignada y práctica {} en vinculación — estudiante {}",
                            empresaId, practica.getId(), estudianteId);
                },
                () -> log.warn("Estudiante {} sin práctica activa", estudianteId)
        );
    }

    // ── Validación de transiciones ─────────────────────────────────────────────

    private EstadoPostulacionTipo parsearEstado(String estado) {
        try {
            return EstadoPostulacionTipo.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + estado);
        }
    }

    private void validarTransicion(EstadoPostulacionTipo actual, EstadoPostulacionTipo nuevo) {
        boolean valido = switch (actual) {
            case POSTULADO     -> nuevo == EstadoPostulacionTipo.EN_SELECCION
                    || nuevo == EstadoPostulacionTipo.RECHAZADO;
            case EN_SELECCION  -> nuevo == EstadoPostulacionTipo.EN_ENTREVISTA
                    || nuevo == EstadoPostulacionTipo.RECHAZADO;
            case EN_ENTREVISTA -> nuevo == EstadoPostulacionTipo.SELECCIONADO
                    || nuevo == EstadoPostulacionTipo.RECHAZADO;
            case SELECCIONADO, RECHAZADO -> false;
        };

        if (!valido) {
            throw new IllegalStateException(
                    "Transición no permitida: " + actual + " -> " + nuevo);
        }
    }

    // ── Consultas ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PostulacionResponseDto obtenerPostulacion(Long id) {
        return mapToResponse(
                postulacionRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Postulacion no encontrada"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostulacionResponseDto> listarTodas() {
        return postulacionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostulacionResponseDto> listarPorVacante(Long vacanteId) {
        return postulacionRepository.findByVacanteId(vacanteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostulacionResponseDto> listarPorEstudiante(Long estudianteId) {
        List<Postulacion> postulaciones = postulacionRepository.findByEstudianteId(estudianteId);
        
        boolean isSeleccionado = postulaciones.stream()
                .anyMatch(p -> p.getEstado() == EstadoPostulacionTipo.SELECCIONADO);
                
        if (isSeleccionado) {
            Optional<Practica> practicaOpt = practicaRepository.findPracticaActivaByEstudiante(estudianteId);
            if (practicaOpt.isPresent() && practicaOpt.get().getEmpresaId() != null) {
                return Collections.emptyList();
            }
        }
        
        return postulaciones.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostulacionResponseDto> listarPorEstudianteEmail(String email) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Email(email)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        return listarPorEstudiante(estudiante.getId());
    }

    @Override
    @Transactional
    public void eliminarPostulacion(Long id) {
        if (!postulacionRepository.existsById(id)) {
            throw new IllegalArgumentException("Postulacion no encontrada");
        }
        postulacionRepository.deleteById(id);
    }

    // ── Mapeo ──────────────────────────────────────────────────────────────────

    private PostulacionResponseDto mapToResponse(Postulacion p) {
        return PostulacionResponseDto.builder()
                .id(p.getId())
                .vacanteId(p.getVacante().getId())
                .tituloVacante(p.getVacante().getTitulo())
                .estudianteId(p.getEstudiante().getId())
                .nombreEstudiante(p.getEstudiante().getUsuario().getNombre())
                .estado(p.getEstado())
                .fechaPostulacion(p.getFechaPostulacion())
                .observaciones(p.getObservaciones())
                .build();
    }
}