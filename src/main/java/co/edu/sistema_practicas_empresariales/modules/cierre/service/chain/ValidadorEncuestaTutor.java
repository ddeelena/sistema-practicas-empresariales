package co.edu.sistema_practicas_empresariales.modules.cierre.service.chain;

import co.edu.sistema_practicas_empresariales.modules.encuesta.model.Encuesta;
import co.edu.sistema_practicas_empresariales.modules.encuesta.repository.EncuestaRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.exception.BusinessException;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import org.springframework.stereotype.Component;

@Component
public class ValidadorEncuestaTutor extends BaseValidadorCierre {

    private final EncuestaRepository encuestaRepository;

    public ValidadorEncuestaTutor(EncuestaRepository encuestaRepository, ValidadorEncuestaEstudiante validadorEncuestaEstudiante) {
        super(validadorEncuestaEstudiante);
        this.encuestaRepository = encuestaRepository;
    }

    @Override
    public void validar(Practica practica) {
        Encuesta encuesta = encuestaRepository.findByPracticaIdAndTipoActorAndActivoTrue(practica.getId(), Encuesta.TipoActor.TUTOR_EMPRESARIAL)
                .orElseThrow(() -> new BusinessException("La encuesta del tutor empresarial no ha sido generada o está pendiente."));

        if (encuesta.getEstado() != Encuesta.EstadoEncuesta.COMPLETADA) {
            throw new BusinessException("La encuesta del tutor empresarial aún no ha sido completada.");
        }

        verificarSiguiente(practica);
    }
}
