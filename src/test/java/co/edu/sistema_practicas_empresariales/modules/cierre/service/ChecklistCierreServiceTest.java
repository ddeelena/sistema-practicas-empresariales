package co.edu.sistema_practicas_empresariales.modules.cierre.service;

import co.edu.sistema_practicas_empresariales.modules.cierre.service.chain.ValidadorNotaDocente;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChecklistCierreServiceTest {

    @Mock
    private ValidadorNotaDocente validadorNotaDocente;

    @InjectMocks
    private ChecklistCierreService checklistCierreService;

    @Test
    void validarChecklistCierreExecutesSequentialChain() {
        Practica practica = new Practica();
        practica.setId(101L);

        checklistCierreService.validarChecklistCierre(practica);

        // Verify the validation starts with the first link
        verify(validadorNotaDocente).validar(practica);
    }
}
