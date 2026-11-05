package mx.condominios.repositories;

import mx.condominios.entities.Alquiler;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RepositorioAlquileres extends JpaRepository<Alquiler, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alquiler a where a.id = :id")
    java.util.Optional<Alquiler> bloquear(@Param("id") Long id);

    @Query("select (count(a) > 0) from Alquiler a where a.condominio.id = :id")
    boolean existePorCondominio(@Param("id") Long id);

    @Query("select a from Alquiler a where a.cuenta.correo = :correo")
    List<Alquiler> buscarPorCorreoDeCuenta(@Param("correo") String correo);
}
