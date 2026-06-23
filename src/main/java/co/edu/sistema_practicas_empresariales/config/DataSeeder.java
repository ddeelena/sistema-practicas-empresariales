package co.edu.sistema_practicas_empresariales.config;

import co.edu.sistema_practicas_empresariales.modules.configuracion.model.Facultad;
import co.edu.sistema_practicas_empresariales.modules.configuracion.model.Programa;
import co.edu.sistema_practicas_empresariales.modules.configuracion.repository.FacultadRepository;
import co.edu.sistema_practicas_empresariales.modules.configuracion.repository.ProgramaRepository;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.CoordinadorPrograma;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Rol;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.ScopeTipo;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.CoordinadorProgramaRepository;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.RolRepository;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FacultadRepository facultadRepository;
    private final ProgramaRepository programaRepository;
    private final CoordinadorProgramaRepository coordinadorProgramaRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {

        try {
            // LIMPIEZA INICIAL: Soft delete para usuarios antiguos de prueba
            jdbcTemplate.execute("UPDATE usuarios SET activo=false, eliminado=true WHERE email IN ('coordinador@universidad.edu.co', 'docente@universidad.edu.co', 'contacto@empresa.com', 'admin@sistema.com', 'empresa@universidad.edu.co')");
            jdbcTemplate.execute("UPDATE programas SET activo=false WHERE nombre LIKE '%Sistemas%'");
        } catch (Exception e) {
            System.err.println("Ignorando error limpieza inicial: " + e.getMessage());
        }

        // Crear roles base si no existen
        Rol rolAdmin = getOrCreateRol(Rol.Nombre.ADMINISTRADOR);
        Rol rolCoordinador = getOrCreateRol(Rol.Nombre.COORDINADOR_PRACTICA);
        Rol rolAcad = getOrCreateRol(Rol.Nombre.COORDINADOR_ACADEMICO);

        // Crear Administrador principal
        if (usuarioRepository.findByEmail("admin@universidad.edu.co").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .email("admin@universidad.edu.co")
                    .password(passwordEncoder.encode("admin123"))
                    .nombre("Super Administrador")
                    .rol(rolAdmin)
                    .build());
        }

        // Crear Facultades
        Facultad facIng = getOrCreateFacultad(1L, "Facultad de Ingeniería");
        Facultad facAdm = getOrCreateFacultad(2L, "Facultad de Ciencias Administrativas");
        Facultad facTur = getOrCreateFacultad(3L, "Facultad de Turismo");

        // Crear Programas Duales
        Programa progInd = getOrCreatePrograma(2L, "Ingeniería Industrial (Dual)", facIng);
        Programa progSoft = getOrCreatePrograma(3L, "Ingeniería de Software (Dual)", facIng);
        Programa progAdm = getOrCreatePrograma(4L, "Administración de Empresas (Dual)", facAdm);
        Programa progTur = getOrCreatePrograma(5L, "Turismo (Dual)", facTur);

        // Crear Coordinadores de Práctica (Asociados a Facultades)
        crearUsuarioConScope("coord.ingenieria@cue.edu.co", "CUE2026*", "Coordinador Prácticas Ingeniería", rolCoordinador, ScopeTipo.FACULTAD, String.valueOf(facIng.getId()));
        crearUsuarioConScope("coord.administracion@cue.edu.co", "CUE2026*", "Coordinador Prácticas Administración", rolCoordinador, ScopeTipo.FACULTAD, String.valueOf(facAdm.getId()));
        crearUsuarioConScope("coord.turismo@cue.edu.co", "CUE2026*", "Coordinador Prácticas Turismo", rolCoordinador, ScopeTipo.FACULTAD, String.valueOf(facTur.getId()));

        // Crear Coordinadores Académicos (Asociados a Programas)
        Usuario uInd = crearUsuarioConScope("academico.industrial@cue.edu.co", "CUE2026*", "Coord. Académico Industrial", rolAcad, ScopeTipo.PROGRAMA, String.valueOf(progInd.getId()));
        Usuario uSoft = crearUsuarioConScope("academico.software@cue.edu.co", "CUE2026*", "Coord. Académico Software", rolAcad, ScopeTipo.PROGRAMA, String.valueOf(progSoft.getId()));
        Usuario uAdm = crearUsuarioConScope("academico.administracion@cue.edu.co", "CUE2026*", "Coord. Académico Administración", rolAcad, ScopeTipo.PROGRAMA, String.valueOf(progAdm.getId()));
        Usuario uTur = crearUsuarioConScope("academico.turismo@cue.edu.co", "CUE2026*", "Coord. Académico Turismo", rolAcad, ScopeTipo.PROGRAMA, String.valueOf(progTur.getId()));

        // Crear roles extra
        Rol rolTutor = getOrCreateRol(Rol.Nombre.TUTOR_EMPRESARIAL);
        Rol rolEmpresa = getOrCreateRol(Rol.Nombre.EMPRESA_VINCULADA);
        Rol rolSecretaria = getOrCreateRol(Rol.Nombre.SECRETARIA_COORDINACION);

        // Crear Coordinador Académico General (como antes)
        crearUsuarioConScope("coordinador_academico@universidad.edu.co", "CUE2026*", "Coordinador Académico General", rolAcad, ScopeTipo.GLOBAL, null);

        // Crear Tutor Empresarial, Empresa y Secretaria
        crearUsuarioConScope("tutor_empresarial@empresa.com", "CUE2026*", "Tutor Empresarial Prueba", rolTutor, ScopeTipo.EMPRESA, "1");
        crearUsuarioConScope("empresa@universidad.edu.co", "CUE2026*", "Empresa Prueba", rolEmpresa, ScopeTipo.EMPRESA, "1");
        crearUsuarioConScope("secretaria@universidad.edu.co", "CUE2026*", "Secretaria Prácticas", rolSecretaria, ScopeTipo.GLOBAL, null);

        // Asignar Programas a Coordinadores Académicos en tabla intermedia
        asignarProgramaACoordinador(uInd, progInd);
        asignarProgramaACoordinador(uSoft, progSoft);
        asignarProgramaACoordinador(uAdm, progAdm);
        asignarProgramaACoordinador(uTur, progTur);
    }

    private Rol getOrCreateRol(Rol.Nombre nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private Facultad getOrCreateFacultad(Long id, String nombre) {
        return facultadRepository.findById(id).orElseGet(() -> 
            facultadRepository.save(Facultad.builder().nombre(nombre).activo(true).build())
        );
    }

    private Programa getOrCreatePrograma(Long id, String nombre, Facultad facultad) {
        return programaRepository.findById(id).orElseGet(() -> 
            programaRepository.save(Programa.builder().nombre(nombre).facultad(facultad).activo(true).build())
        );
    }

    private Usuario crearUsuarioConScope(String email, String password, String nombre, Rol rol, ScopeTipo scopeTipo, String scopeValorId) {
        if (usuarioRepository.findByEmail(email).isEmpty()) {
            Usuario u = Usuario.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .nombre(nombre)
                    .rol(rol)
                    .scopeTipo(scopeTipo)
                    .scopeValorId(scopeValorId)
                    .debeCambiarPassword(true)
                    .activo(true)
                    .eliminado(false)
                    .build();
            return usuarioRepository.save(u);
        }
        return usuarioRepository.findByEmail(email).get();
    }

    private void asignarProgramaACoordinador(Usuario usuario, Programa programa) {
        boolean exists = coordinadorProgramaRepository.findAll().stream()
                .anyMatch(cp -> cp.getUsuario().getId().equals(usuario.getId()) && cp.getPrograma().getId().equals(programa.getId()));
        if (!exists) {
            coordinadorProgramaRepository.save(CoordinadorPrograma.builder()
                    .usuario(usuario)
                    .programa(programa)
                    .build());
        }
    }
}
