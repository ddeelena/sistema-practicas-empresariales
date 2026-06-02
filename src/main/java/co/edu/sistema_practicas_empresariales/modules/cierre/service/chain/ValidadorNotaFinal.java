package co.edu.sistema_practicas_empresariales.modules.cierre.service.chain;

import co.edu.sistema_practicas_empresariales.modules.evaluacion.model.Evaluacion;
import co.edu.sistema_practicas_empresariales.modules.evaluacion.repository.EvaluacionRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.exception.BusinessException;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import org.springframework.stereotype.Component;

@Component
public class ValidadorNotaFinal extends BaseValidadorCierre {

    private final EvaluacionRepository evaluacionRepository;

    public ValidadorNotaFinal(EvaluacionRepository evaluacionRepository, ValidadorEncuestaTutor validadorEncuestaTutor) {
        super(validadorEncuestaTutor);
        this.evaluacionRepository = evaluacionRepository;
    }

    @Override
    public void validar(Practica practica) {
        Evaluacion evaluacion = evaluacionRepository.findByPracticaIdAndActivoTrue(practica.getId())
                .orElseThrow(() -> new BusinessException("No se ha registrado ninguna evaluación para esta práctica."));

        if (evaluacion.getNotaFinal() == null) {
            throw new BusinessException("El coordinador de práctica aún no ha registrado la nota final definitiva.");
        }

        verificarSiguiente(practica);
    }
}
