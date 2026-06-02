package co.edu.sistema_practicas_empresariales.modules.cierre.service;

import co.edu.sistema_practicas_empresariales.modules.cierre.service.chain.ValidadorNotaDocente;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChecklistCierreService {

    private final ValidadorNotaDocente validadorNotaDocente;

    public void validarChecklistCierre(Practica practica) {
        // Iniciar la validación secuencial usando la cadena pre-configurada e inmutable
        validadorNotaDocente.validar(practica);
    }
}
