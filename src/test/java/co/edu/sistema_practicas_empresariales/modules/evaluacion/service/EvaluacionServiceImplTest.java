package co.edu.sistema_practicas_empresariales.modules.evaluacion.service;

import co.edu.sistema_practicas_empresariales.modules.configuracion.model.Programa;
import co.edu.sistema_practicas_empresariales.modules.configuracion.model.ProgramaParametro;
import co.edu.sistema_practicas_empresariales.modules.configuracion.repository.ProgramaParametroRepository;
import co.edu.sistema_practicas_empresariales.modules.empresa.model.TutorEmpresarial;
import co.edu.sistema_practicas_empresariales.modules.empresa.repository.TutorEmpresarialRepository;
import co.edu.sistema_practicas_empresariales.modules.estudiante.model.Estudiante;
import co.edu.sistema_practicas_empresariales.modules.evaluacion.dto.EvaluacionResponse;
import co.edu.sistema_practicas_empresariales.modules.evaluacion.model.Evaluacion;
import co.edu.sistema_practicas_empresariales.modules.evaluacion.repository.EvaluacionRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.exception.BusinessException;
import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;
import co.edu.sistema_practicas_empresariales.modules.practica.repository.PracticaRepository;
import co.edu.sistema_practicas_empresariales.modules.practica.state.EstadoPracticaTipo;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluacionServiceImplTest {

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private PracticaRepository practicaRepository;

    @Mock
    private TutorEmpresarialRepository tutorEmpresarialRepository;

    @Mock
    private ProgramaParametroRepository programaParametroRepository;

    @InjectMocks
    private EvaluacionServiceImpl evaluacionService;

    private Practica practica;
    private Usuario docente;
    private TutorEmpresarial tutor;
    private ProgramaParametro programaParametro;

    @BeforeEach
    void setUp() {
        docente = new Usuario();
        docente.setId(1L);
        docente.setEmail("docente@correo.edu.co");
        docente.setNombre("Docente Asesor");

        tutor = new TutorEmpresarial();
        tutor.setId(2L);
        tutor.setCorreo("tutor@empresa.com");

        Programa programa = new Programa();
        programa.setId(10L);

        programaParametro = new ProgramaParametro();
        programaParametro.setId(100L);
        programaParametro.setPrograma(programa);
        programaParametro.setNotaMinimaAprobacion(BigDecimal.valueOf(3.00));

        Estudiante estudiante = new Estudiante();
        estudiante.setId(5L);
        estudiante.setPrograma(programa);
        estudiante.setUsuario(new Usuario());
        estudiante.getUsuario().setNombre("Estudiante Pepito");

        practica = new Practica();
        practica.setId(20L);
        practica.setEstudiante(estudiante);
        practica.setDocenteAsesor(docente);
        practica.setTutorEmpresarialId(tutor.getId());
        practica.setEstado(EstadoPracticaTipo.EN_PRACTICA);
    }

    @Test
    void registrarNotaDocente_ConExito() {
        BigDecimal nota = BigDecimal.valueOf(4.5);
        Evaluacion evalEsperada = Evaluacion.builder()
                .id(1L)
                .practica(practica)
                .notaDocente(nota)
                .observacionesDocente("Buen desempeño")
                .activo(true)
                .build();

        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(evaluacionRepository.findByPracticaIdAndActivoTrue(20L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evalEsperada);

        EvaluacionResponse res = evaluacionService.registrarNotaDocente(20L, nota, "Buen desempeño", "docente@correo.edu.co");

        assertNotNull(res);
        assertEquals(nota, res.getNotaDocente());
        verify(evaluacionRepository, times(1)).save(any(Evaluacion.class));
    }

    @Test
    void registrarNotaDocente_FueraDeRango_LanzaExcepcion() {
        BigDecimal notaInvalida = BigDecimal.valueOf(5.1);
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));

        assertThrows(BusinessException.class, () ->
                evaluacionService.registrarNotaDocente(20L, notaInvalida, "Excelente", "docente@correo.edu.co")
        );
    }

    @Test
    void registrarNotaDocente_UsuarioNoAsesor_LanzaExcepcion() {
        BigDecimal nota = BigDecimal.valueOf(4.0);
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));

        assertThrows(BusinessException.class, () ->
                evaluacionService.registrarNotaDocente(20L, nota, "Excelente", "otro_docente@correo.edu.co")
        );
    }

    @Test
    void registrarNotaTutor_ConExito() {
        BigDecimal nota = BigDecimal.valueOf(3.8);
        Evaluacion evalEsperada = Evaluacion.builder()
                .id(1L)
                .practica(practica)
                .notaTutor(nota)
                .activo(true)
                .build();

        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(tutorEmpresarialRepository.findByCorreo("tutor@empresa.com")).thenReturn(Optional.of(tutor));
        when(evaluacionRepository.findByPracticaIdAndActivoTrue(20L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evalEsperada);

        EvaluacionResponse res = evaluacionService.registrarNotaTutor(20L, nota, "Cumple expectativas", "tutor@empresa.com");

        assertNotNull(res);
        assertEquals(nota, res.getNotaTutor());
    }

    @Test
    void registrarNotaFinal_SinNotaDocentePrevia_LanzaExcepcion() {
        BigDecimal nota = BigDecimal.valueOf(4.0);
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(evaluacionRepository.findByPracticaIdAndActivoTrue(20L)).thenReturn(Optional.empty()); // No hay evaluación aún

        assertThrows(BusinessException.class, () ->
                evaluacionService.registrarNotaFinal(20L, nota, "Nota Final", "coordinador@correo.edu.co")
        );
    }

    @Test
    void registrarNotaFinal_ConNotaDocentePrevia_ConExito() {
        BigDecimal notaDocente = BigDecimal.valueOf(4.2);
        BigDecimal notaFinal = BigDecimal.valueOf(4.0);

        Evaluacion evalExistente = Evaluacion.builder()
                .id(1L)
                .practica(practica)
                .notaDocente(notaDocente)
                .activo(true)
                .build();

        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(evaluacionRepository.findByPracticaIdAndActivoTrue(20L)).thenReturn(Optional.of(evalExistente));
        when(programaParametroRepository.findByProgramaId(10L)).thenReturn(Optional.of(programaParametro));

        EvaluacionResponse res = evaluacionService.registrarNotaFinal(20L, notaFinal, "Aprobado final", "coordinador@correo.edu.co");

        assertNotNull(res);
        assertEquals(notaFinal, res.getNotaFinal());
        verify(practicaRepository, times(1)).save(practica);
    }
}
