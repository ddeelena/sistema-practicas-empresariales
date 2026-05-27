package co.edu.sistema_practicas_empresariales.modules.usuario.service;

import co.edu.sistema_practicas_empresariales.modules.usuario.dto.UsuarioDto;
import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioFacadeImpl implements UsuarioFacade {

    private final UsuarioService usuarioService;

    private UsuarioDto mapToDto(Usuario u) {
        return UsuarioDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nombre(u.getNombre())
                .activo(u.isActivo())
                .rol(u.getRol() != null ? u.getRol().getNombre() : null)
                .build();
    }

    private Usuario mapToEntity(UsuarioDto dto) {
        Usuario.UsuarioBuilder builder = Usuario.builder()
                .email(dto.getEmail())
                .nombre(dto.getNombre())
                .activo(dto.isActivo());
        if (dto.getId() != null) {
            builder.id(dto.getId());
        }
        // El rol se asignará en el servicio mediante búsqueda por nombre.
        return builder.build();
    }

    @Override
    public List<UsuarioDto> obtenerTodos() {
        return usuarioService.obtenerTodos()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDto obtenerPorId(Long id) {
        return mapToDto(usuarioService.obtenerPorId(id));
    }

    @Override
    public UsuarioDto crear(UsuarioDto usuarioDto) {
        Usuario guardado = usuarioService.crear(mapToEntity(usuarioDto));
        return mapToDto(guardado);
    }

    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) {
        Usuario actualizado = usuarioService.actualizar(id, mapToEntity(usuarioDto));
        return mapToDto(actualizado);
    }

    @Override
    public void eliminar(Long id) {
        usuarioService.eliminar(id);
    }
}
