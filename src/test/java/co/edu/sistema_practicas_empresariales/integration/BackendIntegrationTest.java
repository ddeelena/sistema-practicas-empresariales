package co.edu.sistema_practicas_empresariales.integration;

import co.edu.sistema_practicas_empresariales.modules.configuracion.controller.FacultadController;
import co.edu.sistema_practicas_empresariales.modules.configuracion.controller.ProgramaController;
import co.edu.sistema_practicas_empresariales.modules.empresa.controller.EmpresaController;
import co.edu.sistema_practicas_empresariales.modules.vacante.controller.VacanteController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class BackendIntegrationTest {

    @Autowired
    private FacultadController facultadController;

    @Autowired
    private ProgramaController programaController;

    @Autowired
    private EmpresaController empresaController;

    @Autowired
    private VacanteController vacanteController;

    @Test
    @DisplayName("Test: Los Controladores Principales Levantan (Context Loads)")
    void testControladoresPrincipales() {
        assertNotNull(facultadController);
        assertNotNull(programaController);
        assertNotNull(empresaController);
        assertNotNull(vacanteController);
    }
}
