package co.edu.sistema_practicas_empresariales.modules.usuario.service;

import co.edu.sistema_practicas_empresariales.modules.usuario.dto.UsuarioDto;
import java.util.List;

public interface UsuarioFacade {
    List<UsuarioDto> obtenerTodos();
    UsuarioDto obtenerPorId(Long id);
    UsuarioDto crear(UsuarioDto usuarioDto);
    UsuarioDto actualizar(Long id, UsuarioDto usuarioDto);
    void eliminar(Long id);
}
