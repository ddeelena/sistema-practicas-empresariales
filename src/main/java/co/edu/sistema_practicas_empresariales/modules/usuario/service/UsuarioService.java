package co.edu.sistema_practicas_empresariales.modules.usuario.service;

import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import java.util.List;

public interface UsuarioService {
    List<Usuario> obtenerTodos();
    Usuario obtenerPorId(Long id);
    Usuario crear(Usuario usuario);
    Usuario actualizar(Long id, Usuario usuario);
    void eliminar(Long id);
}
