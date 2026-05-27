package co.edu.sistema_practicas_empresariales.modules.usuario.repository;

import co.edu.sistema_practicas_empresariales.modules.usuario.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(Rol.Nombre nombre);
}
