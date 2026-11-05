package mx.condominios.businessLayer;

import mx.condominios.entities.Alquiler;
import mx.condominios.repositories.RepositorioAlquileres;
import mx.condominios.repositories.RepositorioCondominios;
import mx.condominios.repositories.RepositorioCuentas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ServicioAlquileres {
    private final RepositorioCondominios condominios;
    private final RepositorioAlquileres alquileres;
    private final RepositorioCuentas cuentas;

    public ServicioAlquileres(RepositorioCondominios c, RepositorioAlquileres r, RepositorioCuentas a) {
        condominios = c;
        alquileres = r;
        cuentas = a;
    }

    @Transactional
    public void alquilar(Long id, String correo, LocalDate fecha) {
        var condominio = condominios.bloquearPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("El condominio no existe."));
        if (fecha == null || fecha.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Selecciona una fecha de inicio a partir de hoy.");
        if (alquileres.existePorCondominio(id))
            throw new IllegalArgumentException("Este condominio ya está alquilado.");
        var alquiler = new Alquiler();

        alquiler.condominio = condominio;

        alquiler.cuenta = cuentas.buscarPorCorreo(correo).orElseThrow();
        alquiler.fechaInicio = fecha;
        
        alquiler.folio = "HAB-" + UUID.randomUUID().toString().toUpperCase();
        alquileres.save(alquiler);
    }
}
