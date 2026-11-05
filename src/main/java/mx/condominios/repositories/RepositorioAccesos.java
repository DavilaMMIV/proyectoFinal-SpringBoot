package mx.condominios.repositories;

import mx.condominios.entities.Acceso;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface RepositorioAccesos extends JpaRepository<Acceso, Long> {
    List<Acceso> findAllByOrderByIdDesc();

    List<Acceso> findByAlquilerCuentaCorreoOrderByIdDesc(String correo);

    boolean existsByAlquilerIdAndTipoAndEntradaIsNotNullAndSalidaIsNull(Long id, String tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acceso a where a.id = :id")
    Optional<Acceso> bloquear(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acceso a where a.codigo = :codigo")
    Optional<Acceso> buscarCodigo(@Param("codigo") String codigo);
}
