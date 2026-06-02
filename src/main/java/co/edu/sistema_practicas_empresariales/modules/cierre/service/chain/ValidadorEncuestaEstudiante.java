package co.edu.sistema_practicas_empresariales.modules.cierre.service.chain;

import co.edu.sistema_practicas_empresariales.modules.encuesta.model.Encuesta;
import co.edu.sistema_practicas_empresariales.modules.encuesta.repository.EncuestaRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.exception.BusinessException;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import org.springframework.stereotype.Component;

@Component
public class ValidadorEncuestaEstudiante extends BaseValidadorCierre {

    private final EncuestaRepository encuestaRepository;

    public ValidadorEncuestaEstudiante(EncuestaRepository encuestaRepository, ValidadorDocumentos validadorDocumentos) {
        super(validadorDocumentos);
        this.encuestaRepository = encuestaRepository;
    }

    @Override
    public void validar(Practica practica) {
        Encuesta encuesta = encuestaRepository.findByPracticaIdAndTipoActorAndActivoTrue(practica.getId(), Encuesta.TipoActor.ESTUDIANTE)
                .orElseThrow(() -> new BusinessException("La autoevaluación y encuesta del estudiante no han sido generadas o están pendientes."));

        if (encuesta.getEstado() != Encuesta.EstadoEncuesta.COMPLETADA) {
            throw new BusinessException("La autoevaluación y encuesta del estudiante aún no han sido completadas.");
        }

        verificarSiguiente(practica);
    }
}
