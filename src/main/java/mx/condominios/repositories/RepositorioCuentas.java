package mx.condominios.repositories;

import mx.condominios.entities.Cuenta;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RepositorioCuentas extends JpaRepository<Cuenta, Long> {
    @Query("select c from Cuenta c where c.correo = :correo")
    Optional<Cuenta> buscarPorCorreo(@Param("correo") String correo);
}
