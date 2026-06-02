package co.edu.sistema_practicas_empresariales.modules.cierre.service;

import co.edu.sistema_practicas_empresariales.modules.cierre.service.chain.*;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistCierreServiceTest {

    @Mock private ValidadorNotaDocente validadorNotaDocente;
    @Mock private ValidadorNotaTutor validadorNotaTutor;
    @Mock private ValidadorNotaFinal validadorNotaFinal;
    @Mock private ValidadorEncuestaTutor validadorEncuestaTutor;
    @Mock private ValidadorEncuestaEstudiante validadorEncuestaEstudiante;
    @Mock private ValidadorDocumentos validadorDocumentos;

    @InjectMocks
    private ChecklistCierreService checklistCierreService;

    @Test
    void validarChecklistCierre_ExecutesSequentialChain() {
        Practica practica = new Practica();
        practica.setId(101L);

        checklistCierreService.validarChecklistCierre(practica);

        // Verify next elements in the chain are correctly set
        verify(validadorNotaDocente).setSiguiente(validadorNotaTutor);
        verify(validadorNotaTutor).setSiguiente(validadorNotaFinal);
        verify(validadorNotaFinal).setSiguiente(validadorEncuestaTutor);
        verify(validadorEncuestaTutor).setSiguiente(validadorEncuestaEstudiante);
        verify(validadorEncuestaEstudiante).setSiguiente(validadorDocumentos);
        verify(validadorDocumentos).setSiguiente(null);

        // Verify the validation starts with the first link
        verify(validadorNotaDocente).validar(practica);
    }
}
