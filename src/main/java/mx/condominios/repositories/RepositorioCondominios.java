package mx.condominios.repositories;

import mx.condominios.entities.Condominio;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RepositorioCondominios extends JpaRepository<Condominio, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Condominio c where c.id = :id")
    Optional<Condominio> bloquearPorId(Long id);
}
