package co.edu.sistema_practicas_empresariales.modules.estudiante.service;

import co.edu.sistema_practicas_empresariales.modules.configuracion.model.Programa;
import co.edu.sistema_practicas_empresariales.modules.configuracion.repository.ProgramaRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.dto.EstudianteRequest;
import co.edu.sistema_practicas_empresariales.modules.estudiante.dto.EstudianteResponse;
import co.edu.sistema_practicas_empresariales.modules.estudiante.dto.PracticaResponse;
import co.edu.sistema_practicas_empresariales.modules.estudiante.event.AptitudEvaluadaEvent;
import co.edu.sistema_practicas_empresariales.modules.estudiante.event.EstudianteRegistradoEvent;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.Estudiante;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.ProgramaRequisito;
import co.edu.sistema_practicas_empresariales.modules.estudiante.repository.EstudianteRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.repository.ProgramaRequisitoRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.service.chain.CreditosHandler;
import co.edu.sistema_practicas_empresariales.modules.estudiante.service.chain.DocumentosHandler;
import co.edu.sistema_practicas_empresariales.modules.estudiante.service.chain.PracticaAnteriorHandler;
import co.edu.sistema_practicas_empresariales.modules.estudiante.service.chain.PromedioHandler;
import co.edu.sistema_practicas_empresariales.modules.estudiante.service.chain.ValidadorAptitudHandler;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.PracticaRepository;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del patrón Facade para el módulo de Estudiante.
 * Orquesta la lógica de negocio coordinando repositorios, cadena de validación
 * (Chain of Responsibility) y publicación de eventos (Observer).
 */
@Service
@RequiredArgsConstructor
public class EstudianteFacadeImpl implements EstudianteFacade {

    private static final String ESTUDIANTE_NO_ENCONTRADO_MSG = "Estudiante no encontrado con ID: ";

    private final EstudianteRepository estudianteRepository;
    private final ProgramaRequisitoRepository programaRequisitoRepository;
    private final PracticaRepository practicaRepository;
    private final ProgramaRepository programaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EstudianteResponse registrarEstudiante(EstudianteRequest request) {
        if (estudianteRepository.existsByIdentificacion(request.getIdentificacion())) {
            throw new IllegalArgumentException("Ya existe un estudiante con la identificación: " + request.getIdentificacion());
        }

        Programa programa = programaRepository.findById(request.getProgramaId())
                .orElseThrow(() -> new IllegalArgumentException("Programa no encontrado con ID: " + request.getProgramaId()));

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + request.getEmail()));

        Estudiante estudiante = Estudiante.builder()
                .usuario(usuario)
                .identificacion(request.getIdentificacion())
                .telefono(request.getTelefono())
                .contactoEmergencia(request.getContactoEmergencia())
                .programa(programa)
                .semestre(request.getSemestre())
                .creditosAprobados(request.getCreditosAprobados())
                .promedioAcumulado(request.getPromedioAcumulado())
                .build();

        estudiante = estudianteRepository.save(estudiante);

        // Patrón Observer: publicar evento de registro
        eventPublisher.publishEvent(new EstudianteRegistradoEvent(this, estudiante));

        return mapToResponse(estudiante);
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtenerPorId(Long id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ESTUDIANTE_NO_ENCONTRADO_MSG + id));
        return mapToResponse(estudiante);
    }

    @Override
    @Transactional(readOnly = true)
    public EstudianteResponse obtenerPorUsuarioId(Long usuarioId) {
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado para el usuario ID: " + usuarioId));
        return mapToResponse(estudiante);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarTodos() {
        return estudianteRepository.findByActivoTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarPorPrograma(Long programaId) {
        return estudianteRepository.findByProgramaIdAndActivoTrue(programaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarAptos() {
        return estudianteRepository.findAptos().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EstudianteResponse actualizarEstudiante(Long id, EstudianteRequest request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ESTUDIANTE_NO_ENCONTRADO_MSG + id));

        if (request.getTelefono() != null) {
            estudiante.setTelefono(request.getTelefono());
        }
        if (request.getContactoEmergencia() != null) {
            estudiante.setContactoEmergencia(request.getContactoEmergencia());
        }
        if (request.getSemestre() > 0) {
            estudiante.setSemestre(request.getSemestre());
        }
        if (request.getCreditosAprobados() > 0) {
            estudiante.setCreditosAprobados(request.getCreditosAprobados());
        }
        if (request.getPromedioAcumulado() != null) {
            estudiante.setPromedioAcumulado(request.getPromedioAcumulado());
        }

        estudiante = estudianteRepository.save(estudiante);
        return mapToResponse(estudiante);
    }

    /**
     * Evalúa la aptitud del estudiante para una práctica específica.
     * Utiliza el patrón Chain of Responsibility para ejecutar las validaciones
     * y el patrón Strategy dentro de cada handler para aplicar las reglas
     * configurables por programa.
     */
    @Override
    @Transactional
    public EstudianteResponse evaluarAptitud(Long estudianteId, int numeroPractica) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException(ESTUDIANTE_NO_ENCONTRADO_MSG + estudianteId));

        ProgramaRequisito requisito = programaRequisitoRepository
                .findByProgramaIdAndNumeroPractica(estudiante.getPrograma().getId(), numeroPractica)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontraron requisitos para la práctica " + numeroPractica +
                        " del programa " + estudiante.getPrograma().getNombre()));

        List<Practica> historial = practicaRepository
                .findByEstudianteIdOrderByNumeroPracticaAsc(estudianteId);

        // Construir la cadena de validación (Chain of Responsibility)
        ValidadorAptitudHandler cadena = construirCadenaValidacion();

        List<String> errores = new ArrayList<>();
        cadena.procesar(estudiante, requisito, historial, errores);

        boolean esApto = errores.isEmpty();
        if (esApto) {
            estudiante.setEstadoAptitud(Estudiante.EstadoAptitud.APTO);
        } else {
            estudiante.setEstadoAptitud(Estudiante.EstadoAptitud.NO_APTO);
        }

        estudianteRepository.save(estudiante);

        // Patrón Observer: publicar evento de aptitud evaluada
        String motivo = esApto ? "Cumple todos los requisitos" : String.join("; ", errores);
        eventPublisher.publishEvent(new AptitudEvaluadaEvent(this, estudiante, esApto, motivo));

        return mapToResponse(estudiante);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticaResponse> obtenerHistorialPracticas(Long estudianteId) {
        estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException(ESTUDIANTE_NO_ENCONTRADO_MSG + estudianteId));

        return practicaRepository.findByEstudianteIdOrderByNumeroPracticaAsc(estudianteId).stream()
                .map(this::mapToPracticaResponse)
                .collect(Collectors.toList());
    }

    // ========== Métodos privados ==========

    /**
     * Construye la cadena de validación con los handlers correspondientes.
     * El orden importa: créditos → promedio → documentos → práctica anterior.
     */
    private ValidadorAptitudHandler construirCadenaValidacion() {
        ValidadorAptitudHandler creditosHandler = new CreditosHandler();
        ValidadorAptitudHandler promedioHandler = new PromedioHandler();
        ValidadorAptitudHandler documentosHandler = new DocumentosHandler();
        ValidadorAptitudHandler practicaAnteriorHandler = new PracticaAnteriorHandler();

        creditosHandler.setNext(promedioHandler);
        promedioHandler.setNext(documentosHandler);
        documentosHandler.setNext(practicaAnteriorHandler);

        return creditosHandler;
    }

    private EstudianteResponse mapToResponse(Estudiante e) {
        return EstudianteResponse.builder()
                .id(e.getId())
                .nombre(e.getUsuario().getNombre())
                .email(e.getUsuario().getEmail())
                .identificacion(e.getIdentificacion())
                .telefono(e.getTelefono())
                .contactoEmergencia(e.getContactoEmergencia())
                .programaId(e.getPrograma().getId())
                .nombrePrograma(e.getPrograma().getNombre())
                .nombreFacultad(e.getPrograma().getFacultad() != null ? e.getPrograma().getFacultad().getNombre() : null)
                .semestre(e.getSemestre())
                .creditosAprobados(e.getCreditosAprobados())
                .promedioAcumulado(e.getPromedioAcumulado())
                .estadoAptitud(e.getEstadoAptitud().name())
                .estadoPractica(e.getEstadoPractica())
                .activo(e.isActivo())
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }

    private PracticaResponse mapToPracticaResponse(Practica p) {
        return PracticaResponse.builder()
                .id(p.getId())
                .numeroPractica(p.getNumeroPractica())
                .nombre(p.getNombre())
                .materiaNucleo(p.getMateriaNucleo())
                .materiaNucleoCodigo(p.getMateriaNucleoCodigo())
                .duracionSemanas(p.getDuracionSemanas())
                .estado(p.getEstado().name())
                .nombreEmpresa(p.getEmpresaId() != null ? "Empresa ID: " + p.getEmpresaId() : null)
                .nombreDocenteAsesor(p.getDocenteAsesor() != null ? p.getDocenteAsesor().getNombre() : null)
                .nombreTutorEmpresarial(p.getTutorEmpresarialId() != null ? "Tutor ID: " + p.getTutorEmpresarialId() : null)
                .notaFinal(p.getNotaFinal())
                .resultado(p.getResultado())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .build();
    }
}
