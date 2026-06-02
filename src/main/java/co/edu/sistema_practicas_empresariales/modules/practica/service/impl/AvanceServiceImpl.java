package co.edu.sistema_practicas_empresariales.modules.practica.service.impl;

import co.edu.sistema_practicas_empresariales.modules.infraestructura.storage.ArchivoStorageService;
import co.edu.sistema_practicas_empresariales.modules.practica.dto.AvanceDto;
import co.edu.sistema_practicas_empresariales.modules.practica.request.ComentarioDocenteRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.request.CrearAvanceRequest;
import co.edu.sistema_practicas_empresariales.modules.practica.enums.EstadoAvance;
import co.edu.sistema_practicas_empresariales.modules.practica.model.*;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.*;
import co.edu.sistema_practicas_empresariales.modules.practica.service.AvanceService;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de avances del estudiante.
 * Un avance es una entrega libre — el estudiante sube cuando acuerda
 * con su docente (semanal, quincenal, cuando tenga avances, etc).
 * El sistema registra la fecha automáticamente.
 * El docente revisa y comenta cada avance individualmente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AvanceServiceImpl implements AvanceService {

    private final AvanceRepository      avanceRepository;
    private final PracticaRepository    practicaRepository;
    private final ArchivoStorageService storageService;

    @Transactional(readOnly = true)
    public List<AvanceDto> listarPorPractica(Long practicaId) {
        return avanceRepository
                .findByPracticaIdOrderByCreatedAtDesc(practicaId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvanceDto> listarPendientesPorDocente(Long docenteId) {
        return avanceRepository
                .findByDocenteIdAndEstado(docenteId, EstadoAvance.PENDIENTE)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public AvanceDto crearAvance(Long practicaId,
                                 CrearAvanceRequest req,
                                 MultipartFile archivo) {

        Practica practica = practicaRepository.findById(practicaId)
                .orElseThrow(() -> new RuntimeException("Práctica no encontrada"));

        if (practica.getEstado() != EstadoPracticaTipo.EN_PRACTICA) {
            throw new RuntimeException(
                    "Solo se pueden registrar avances en prácticas EN_PRACTICA. " +
                            "Estado actual: " + practica.getEstado());
        }

        Avance avance = Avance.builder()
                .practica(practica)
                .titulo(req.getTitulo())
                .descripcion(req.getDescripcion())
                .estado(EstadoAvance.PENDIENTE)
                .build();

        // Subir archivo si viene adjunto — Patrón Adapter
        if (archivo != null && !archivo.isEmpty()) {
            String url = storageService.subirArchivo(
                    archivo,
                    "practicas/" + practicaId + "/avances",
                    "avance_" + practicaId + "_" + System.currentTimeMillis()
            );
            avance.setArchivoUrl(url);
            avance.setArchivoFechaCarga(LocalDateTime.now());
        }

        avance = avanceRepository.save(avance);
        log.info("Avance creado — practicaId={} titulo={}", practicaId, req.getTitulo());

        return toDto(avance);
    }

    public AvanceDto agregarComentarioDocente(Long avanceId,
                                              ComentarioDocenteRequest req) {
        Avance avance = avanceRepository.findById(avanceId)
                .orElseThrow(() -> new RuntimeException("Avance no encontrado"));
        avance.setComentarioDocente(req.getComentario());
        avance.setEstado(EstadoAvance.REVISADO);
        return toDto(avanceRepository.save(avance));
    }

    public AvanceDto cambiarEstado(Long avanceId, EstadoAvance nuevoEstado) {
        Avance avance = avanceRepository.findById(avanceId)
                .orElseThrow(() -> new RuntimeException("Avance no encontrado"));
        avance.setEstado(nuevoEstado);
        return toDto(avanceRepository.save(avance));
    }

    public AvanceDto toDto(Avance a) {
        return AvanceDto.builder()
                .id(a.getId())
                .practica_id(a.getPractica().getId())
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