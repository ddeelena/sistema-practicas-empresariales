package co.edu.sistema_practicas_empresariales.modules.practica.service.impl;

import co.edu.sistema_practicas_empresariales.modules.infraestructura.storage.ArchivoStorageService;
import co.edu.sistema_practicas_empresariales.modules.practica.dto.*;
import co.edu.sistema_practicas_empresariales.modules.practica.model.*;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.*;
import co.edu.sistema_practicas_empresariales.modules.practica.request.FechaSustentacionRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.request.NotaFinalRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.service.PracticaService;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.CatalogoPractica;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.Estudiante;
import co.edu.sistema_practicas_empresariales.modules.estudiante.repository.CatalogoPracticaRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.repository.EstudianteRepository;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio principal de prácticas.
 *
 * Patrones aplicados:
 * - State:                el modelo Practica delega comportamiento según su estado
 * - Factory Method:       crearCortesAutomaticos y crearChecklistInicial
 * - Chain of Responsibility: checklist de paz y salvo
 * - Observer:             actualización automática del checklist por eventos
 * - Adapter:              Firebase a través de ArchivoStorageService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PracticaServiceImpl implements PracticaService {

    private final PracticaRepository         practicaRepository;
    private final AvanceRepository           avanceRepository;
    private final ChecklistItemRepository    checklistRepository;
    private final PracticaDocumentoRepository documentoRepository;
    private final CatalogoPracticaRepository catalogoRepository;
    private final EstudianteRepository       estudianteRepository;
    private final UsuarioRepository          usuarioRepository;
    private final ArchivoStorageService      storageService;

    // Claves del checklist
    private static final String CK_NOTA_FINAL     = "nota_final";
    private static final String CK_ENCUESTA_TUTOR = "encuesta_tutor";
    private static final String CK_ENCUESTA_EST   = "encuesta_estudiante";
    private static final String CK_DOCUMENTOS     = "documentos_completos";
    private static final String CK_INFORME        = "informe_final";

    // ─────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PracticaResumenDto> listarTodas() {
        return practicaRepository.findAll().stream()
                .map(this::toResumen).toList();
    }

    @Transactional(readOnly = true)
    public List<PracticaResumenDto> listarPorEstado(EstadoPracticaTipo estado) {
        return practicaRepository.findByEstado(estado).stream()
                .map(this::toResumen).toList();
    }

    @Transactional(readOnly = true)
    public List<PracticaResumenDto> listarPorDocente(Long docenteId) {
        return practicaRepository.findPracticasActivasByDocente(docenteId).stream()
                .map(this::toResumen).toList();
    }

    @Transactional(readOnly = true)
    public PracticaDetalleDto obtenerDetalle(Long id) {
        return toDetalle(buscar(id));
    }

    @Transactional(readOnly = true)
    public PracticaDetalleDto obtenerPracticaActivaEstudiante(Long estudianteId) {
        Practica p = practicaRepository.findPracticaActivaByEstudiante(estudianteId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay práctica activa para el estudiante " + estudianteId));
        return toDetalle(p);
    }

    // ─────────────────────────────────────────────────────────────────
    // CREACIÓN AUTOMÁTICA
    // Patrón Factory Method
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crea la instancia de práctica automáticamente cuando el coordinador
     * académico marca al estudiante como APTO y asigna número de práctica.
     * Patrón Factory Method: inicializa cortes y checklist automáticamente.
     */
    public PracticaDetalleDto crearPracticaAutomatica(Long estudianteId, Long catalogoId) {

        Estudiante      estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        CatalogoPractica catalogo  = catalogoRepository.findById(catalogoId)
                .orElseThrow(() -> new RuntimeException("Catálogo no encontrado"));

        // Verificar que no haya práctica activa del mismo número
        boolean existe = practicaRepository.existsByEstudianteIdAndEstadoNotIn(
                estudianteId,
                List.of(EstadoPracticaTipo.COMPLETADA,
                        EstadoPracticaTipo.REPROBADA,
                        EstadoPracticaTipo.CANCELADA)
        );
        if (existe) {
            throw new RuntimeException("El estudiante ya tiene una práctica activa");
        }

        // Patrón State: estado inicial ASIGNADA_PENDIENTE_INICIO
        Practica practica = Practica.builder()
                .estudiante(estudiante)
                .catalogoPractica(catalogo)
                .numeroPractica(catalogo.getNumeroPractica())
                .nombre(catalogo.getNombre())
                .materiaNucleo(catalogo.getMateriaNucleo())
                .materiaNucleoCodigo(catalogo.getMateriaNucleoCodigo())
                .duracionSemanas(catalogo.getDuracionSemanas())
                .build();

        practica = practicaRepository.save(practica);


        // Factory Method: checklist inicial
        crearChecklistInicial(practica);

        log.info("Práctica creada — estudianteId={} catalogoId={}", estudianteId, catalogoId);
        return toDetalle(practica);
    }

    // ─────────────────────────────────────────────────────────────────
    // TRANSICIONES DE ESTADO
    // Patrón State: delegado al modelo
    // ─────────────────────────────────────────────────────────────────

    /**
     * Inicia el proceso de vinculación con la empresa.
     * Patrón State: delega al comportamiento del estado actual.
     */
    public PracticaDetalleDto iniciarVinculacion(Long practicaId) {
        Practica p = buscar(practicaId);
        p.iniciarVinculacion(); // Patrón State
        return toDetalle(practicaRepository.save(p));
    }

    /**
     * Registra el convenio con la empresa.
     * Patrón State: delega al comportamiento del estado actual.
     */
    public PracticaDetalleDto registrarConvenio(Long practicaId) {
        Practica p = buscar(practicaId);
        p.registrarConvenio(); // Patrón State
        return toDetalle(practicaRepository.save(p));
    }

    /**
     * Activa la práctica — cambia a EN_PRACTICA.
     * Patrón State: delega al comportamiento del estado actual.
     */
    public PracticaDetalleDto activarPractica(Long practicaId) {
        Practica p = buscar(practicaId);
        if (p.getDocenteAsesor() == null) {
            throw new RuntimeException("No se puede activar sin docente asesor asignado");
        }
        p.activarPractica(); // Patrón State
        return toDetalle(practicaRepository.save(p));
    }

    /**
     * Cancela la práctica.
     * Patrón State: delega al comportamiento del estado actual.
     */
    public PracticaDetalleDto cancelar(Long practicaId, String motivo) {
        Practica p = buscar(practicaId);
        p.cancelar(motivo); // Patrón State
        return toDetalle(practicaRepository.save(p));
    }

    // ─────────────────────────────────────────────────────────────────
    // ASIGNACIONES
    // ─────────────────────────────────────────────────────────────────

    public PracticaDetalleDto asignarDocente(Long practicaId, Long docenteId) {
        Practica p  = buscar(practicaId);
        Usuario doc = usuarioRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
        p.setDocenteAsesor(doc);
        return toDetalle(practicaRepository.save(p));
    }

    public PracticaDetalleDto asignarEmpresa(Long practicaId, Long empresaId) {
        Practica p = buscar(practicaId);
        p.setEmpresaId(empresaId);
        return toDetalle(practicaRepository.save(p));
    }

    public PracticaDetalleDto asignarTutor(Long practicaId, Long tutorId) {
        Practica p = buscar(practicaId);
        p.setTutorEmpresarialId(tutorId);
        return toDetalle(practicaRepository.save(p));
    }

    public PracticaDetalleDto registrarFechaSustentacion(Long practicaId,
                                                         FechaSustentacionRequest req) {
        Practica p = buscar(practicaId);
        p.setFechaSustentacion(req.getFechaSustentacion());
        return toDetalle(practicaRepository.save(p));
    }

    // ─────────────────────────────────────────────────────────────────
    // DOCUMENTOS — Firebase Storage
    // Patrón Adapter
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sube un documento a Firebase Storage y lo registra en BD.
     * Patrón Adapter: ArchivoStorageService abstrae Firebase.
     * @param categoria ARL | PLANEADOR | INFORME_EJECUTIVO | PRESENTACION | DOCUMENTO_FINAL
     */
    public PracticaDetalleDto subirDocumento(Long practicaId,
                                             String categoria,
                                             MultipartFile archivo,
                                             String emailUsuario) {
        Practica p = buscar(practicaId);

        String carpeta = "practicas/" + practicaId + "/" + categoria.toLowerCase();
        String nombre  = categoria.toLowerCase() + "_" + practicaId;
        String url     = storageService.subirArchivo(archivo, carpeta, nombre);

        PracticaDocumento doc = PracticaDocumento.builder()
                .practica(p)
                .nombre(archivo.getOriginalFilename())
                .url(url)
                .categoria(categoria)
                .fechaCarga(LocalDateTime.now())
                .cargadoPorEmail(emailUsuario)
                .estado("PENDIENTE")
                .build();

        documentoRepository.save(doc);

        // Observer: actualizar checklist según la categoría
        if ("ARL".equals(categoria)) {
            verificarYActualizarDocumentos(p);
        }
        if ("DOCUMENTO_FINAL".equals(categoria) || "INFORME_EJECUTIVO".equals(categoria)) {
            actualizarChecklist(p.getId(), CK_INFORME);
        }

        log.info("Documento {} subido — practicaId={}", categoria, practicaId);
        return toDetalle(practicaRepository.save(p));
    }

    // ─────────────────────────────────────────────────────────────────
    // NOTA FINAL
    // ─────────────────────────────────────────────────────────────────

    /**
     * Registra la nota final y determina el resultado.
     * Patrón State: delega al modelo para cambiar estado según nota.
     */
    public PracticaDetalleDto registrarNotaFinal(Long practicaId, NotaFinalRequest req) {
        Practica p = buscar(practicaId);

        // Obtener nota mínima del programa (desde parámetros)
        BigDecimal notaMinima = BigDecimal.valueOf(3.0); // valor por defecto
        // TODO: obtener del programa_parametros cuando esté disponible el repo

        p.registrarNotaFinal(req.getNota(), notaMinima); // Patrón State
        actualizarChecklist(practicaId, CK_NOTA_FINAL);

        return toDetalle(practicaRepository.save(p));
    }

    // ─────────────────────────────────────────────────────────────────
    // CHECKLIST PAZ Y SALVO
    // Patrón Chain of Responsibility
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChecklistDto> obtenerChecklist(Long practicaId) {
        return checklistRepository.findByPracticaId(practicaId).stream()
                .map(i -> ChecklistDto.builder()
                        .id(i.getId())
                        .clave(i.getClave())
                        .label(i.getLabel())
                        .completado(i.getCompletado())
                        .build())
                .toList();
    }

    public boolean tienePazYSalvo(Long practicaId) {
        return !checklistRepository.existsByPracticaIdAndCompletadoFalse(practicaId);
    }

    public void marcarEncuestaEstudianteCompletada(Long practicaId) {
        actualizarChecklist(practicaId, CK_ENCUESTA_EST);
    }

    public void marcarEncuestaTutorCompletada(Long practicaId) {
        actualizarChecklist(practicaId, CK_ENCUESTA_TUTOR);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────



    /**
     * Patrón Factory Method: crea el checklist inicial.
     * Patrón Chain of Responsibility: cada ítem es un eslabón.
     */
    private void crearChecklistInicial(Practica practica) {
        List.of(
                new String[]{CK_NOTA_FINAL,     "Nota final registrada"},
                new String[]{CK_ENCUESTA_TUTOR, "Encuesta del tutor completada"},
                new String[]{CK_ENCUESTA_EST,   "Encuesta del estudiante completada"},
                new String[]{CK_DOCUMENTOS,     "Documentos completos (ARL + planeador)"},
                new String[]{CK_INFORME,        "Informe final cargado"}
        ).forEach(item ->
                checklistRepository.save(
                        ChecklistItem.builder()
                                .practica(practica)
                                .clave(item[0])
                                .label(item[1])
                                .completado(false)
                                .build()
                )
        );
    }

    /**
     * Patrón Observer: actualiza el checklist cuando ocurre un evento.
     */
    private void actualizarChecklist(Long practicaId, String clave) {
        checklistRepository.findByPracticaIdAndClave(practicaId, clave)
                .ifPresent(item -> {
                    item.setCompletado(true);
                    checklistRepository.save(item);
                });
    }

    /** Verifica si todos los documentos requeridos están cargados */
    private void verificarYActualizarDocumentos(Practica practica) {
        List<String> categoriasRequeridas = List.of("ARL", "PLANEADOR");
        List<String> categoriasActuales   = documentoRepository
                .findByPracticaId(practica.getId())
                .stream()
                .map(PracticaDocumento::getCategoria)
                .toList();

        boolean completo = categoriasRequeridas.stream()
                .allMatch(categoriasActuales::contains);

        if (completo) {
            actualizarChecklist(practica.getId(), CK_DOCUMENTOS);
        }
    }

    private Practica buscar(Long id) {
        return practicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Práctica no encontrada: " + id));
    }

    // ─────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────

    private PracticaResumenDto toResumen(Practica p) {
        return PracticaResumenDto.builder()
                .id(p.getId())
                .nombrePractica(p.getNombre())
                .materiaNucleo(p.getMateriaNucleo())
                .estudianteId(p.getEstudiante().getId())
                .nombreEstudiante(p.getEstudiante().getUsuario().getNombre())
                .empresaId(p.getEmpresaId())
                .docenteId(p.getDocenteAsesor() != null ? p.getDocenteAsesor().getId() : null)
                .nombreDocente(p.getDocenteAsesor() != null ? p.getDocenteAsesor().getNombre() : null)
                .estado(p.getEstado())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .tienePazYSalvo(tienePazYSalvo(p.getId()))
                .build();
    }

    private PracticaDetalleDto toDetalle(Practica p) {

        List<ChecklistDto> checklist = checklistRepository
                .findByPracticaId(p.getId())
                .stream()
                .map(i -> ChecklistDto.builder()
                        .id(i.getId())
                        .clave(i.getClave())
                        .label(i.getLabel())
                        .completado(i.getCompletado())
                        .build())
                .toList();

        List<DocumentoPracticaDto> docs = documentoRepository
                .findByPracticaId(p.getId())
                .stream()
                .map(d -> DocumentoPracticaDto.builder()
                        .id(d.getId())
                        .nombre(d.getNombre())
                        .url(d.getUrl())
                        .categoria(d.getCategoria())
                        .fechaCarga(d.getFechaCarga())
                        .cargadoPorEmail(d.getCargadoPorEmail())
                        .estado(d.getEstado())
                        .build())
                .toList();

        return PracticaDetalleDto.builder()
                .id(p.getId())
                .estado(p.getEstado())
                .catalogoPracticaId(p.getCatalogoPractica() != null
                        ? p.getCatalogoPractica().getId() : null)
                .nombrePractica(p.getNombre())
                .materiaNucleo(p.getMateriaNucleo())
                .materiaNucleoCodigo(p.getMateriaNucleoCodigo())
                .numeroPractica(p.getNumeroPractica())
                .duracionSemanas(p.getDuracionSemanas())
                .estudianteId(p.getEstudiante().getId())
                .nombreEstudiante(p.getEstudiante().getUsuario().getNombre())
                .empresaId(p.getEmpresaId())
                .docenteId(p.getDocenteAsesor() != null ? p.getDocenteAsesor().getId() : null)
                .nombreDocente(p.getDocenteAsesor() != null ? p.getDocenteAsesor().getNombre() : null)
                .tutorId(p.getTutorEmpresarialId())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .fechaSustentacion(p.getFechaSustentacion())
                .notaFinal(p.getNotaFinal())
                .resultado(p.getResultado())
                .checklist(checklist)
                .tienePazYSalvo(tienePazYSalvo(p.getId()))
                .build();
    }

    private AvanceDto toAvanceDto(Avance a) {
        return AvanceDto.builder()
                .id(a.getId())
                .titulo(a.getTitulo())
                .descripcion(a.getDescripcion())
                .archivoUrl(a.getArchivoUrl())
                .archivoFechaCarga(a.getArchivoFechaCarga())
                .comentarioDocente(a.getComentarioDocente())
                .estado(a.getEstado())
                .createdAt(a.getCreatedAt())
                .build();
    }
}