package co.edu.sistema_practicas_empresariales.modules.cierre.service.chain;

import co.edu.sistema_practicas_empresariales.modules.practica.exception.BusinessException;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import co.edu.sistema_practicas_empresariales.modules.practica.model.PracticaDocumento;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.PracticaDocumentoRepository;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ValidadorDocumentos extends BaseValidadorCierre {

    private final PracticaDocumentoRepository practicaDocumentoRepository;

    public ValidadorDocumentos(PracticaDocumentoRepository practicaDocumentoRepository) {
        super(null);
        this.practicaDocumentoRepository = practicaDocumentoRepository;
    }

    private static final List<String> CATEGORIAS_OBLIGATORIAS = Arrays.asList(
            "ARL", "PLANEADOR", "INFORME_EJECUTIVO", "PRESENTACION", "DOCUMENTO_FINAL"
    );

    @Override
    public void validar(Practica practica) {
        List<PracticaDocumento> documentos = practicaDocumentoRepository.findByPracticaId(practica.getId());

        // Agrupar por categoría
        Map<String, List<PracticaDocumento>> docsPorCategoria = documentos.stream()
                .collect(Collectors.groupingBy(d -> d.getCategoria().toUpperCase()));

        // Verificar que existan todas las categorías obligatorias
        for (String categoria : CATEGORIAS_OBLIGATORIAS) {
            List<PracticaDocumento> docs = docsPorCategoria.get(categoria);
            if (docs == null || docs.isEmpty()) {
                throw new BusinessException("Falta cargar el documento obligatorio de la categoría: " + categoria);
            }

            // Verificar que al menos uno esté aprobado
            boolean algunoAprobado = docs.stream().anyMatch(d -> "APROBADO".equalsIgnoreCase(d.getEstado()));
            if (!algunoAprobado) {
                throw new BusinessException("El documento de la categoría " + categoria + " aún no ha sido aprobado.");
            }
        }

        verificarSiguiente(practica);
    }
}
