package co.edu.sistema_practicas_empresariales.modules.usuario.service;

import co.edu.sistema_practicas_empresariales.modules.usuario.model.Usuario;
import co.edu.sistema_practicas_empresariales.modules.usuario.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        return opt.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + id));
    }

    @Override
    public Usuario crear(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario actualizar(Long id, Usuario usuario) {
        Usuario existente = obtenerPorId(id);
        // Actualizamos campos permitidos
        existente.setEmail(usuario.getEmail());
        existente.setNombre(usuario.getNombre());
        existente.setActivo(usuario.isActivo());
        existente.setRol(usuario.getRol());
        existente.setPassword(usuario.getPassword()); // se asume que ya está codificado
        return usuarioRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Usuario existente = obtenerPorId(id);
        usuarioRepository.delete(existente);
    }
}
