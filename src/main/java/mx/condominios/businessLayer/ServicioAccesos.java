package mx.condominios.businessLayer;

import mx.condominios.entities.Acceso;
import mx.condominios.entities.Alquiler;
import mx.condominios.repositories.RepositorioAccesos;
import mx.condominios.repositories.RepositorioAlquileres;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Service
@Transactional
public class ServicioAccesos {
    private final RepositorioAccesos accesos;
    private final RepositorioAlquileres alquileres;

    public ServicioAccesos(RepositorioAccesos accesos, RepositorioAlquileres alquileres) {
        this.accesos = accesos;
        this.alquileres = alquileres;
    }

    public Acceso autorizar(Long alquilerId, String correo, String nombre, LocalDate fecha) {
        var alquiler = alquileres.bloquear(alquilerId)
                .orElseThrow(() -> new IllegalArgumentException("Alquiler inexistente."));
        if (!alquiler.cuenta.correo.equals(correo))
            throw new IllegalArgumentException("Este alquiler no te pertenece.");
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 120)
            throw new IllegalArgumentException("Escribe el nombre del visitante (máximo 120 caracteres).");
        if (fecha == null || fecha.isBefore(LocalDate.now()) || fecha.isBefore(alquiler.fechaInicio))
            throw new IllegalArgumentException("La visita debe ser a partir de hoy y del inicio del alquiler.");
        var acceso = new Acceso();
        acceso.alquiler = alquiler;
        acceso.persona = nombre.trim();
        acceso.tipo = "VISITANTE";
        acceso.fechaVisita = fecha;
        acceso.codigo = UUID.randomUUID().toString();
        return accesos.save(acceso);
    }

    public void cancelar(Long id, String correo) {
        var acceso = obtener(id);
        if (!acceso.alquiler.cuenta.correo.equals(correo) || !acceso.tipo.equals("VISITANTE"))
            throw new IllegalArgumentException("No puedes cancelar esta autorización.");
        if (acceso.entrada != null || acceso.cancelado)
            throw new IllegalArgumentException("La visita ya fue utilizada o cancelada.");
        acceso.cancelado = true;
    }

    public void entradaVisita(String codigo, String operador) {
        var acceso = accesos.buscarCodigo(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Código de visita inválido."));
        if (acceso.cancelado || acceso.entrada != null || !LocalDate.now().equals(acceso.fechaVisita))
            throw new IllegalArgumentException("La autorización está cancelada, usada o fuera de fecha.");
        acceso.entrada = LocalDateTime.now();
        acceso.registradoPor = operador;
    }

    public void entradaResidente(Long id, String operador) {
        var alquiler = alquileres.bloquear(id).orElseThrow(() -> new IllegalArgumentException("Alquiler inexistente."));
        if (alquiler.fechaInicio.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("El alquiler aún no inicia.");
        if (accesos.existsByAlquilerIdAndTipoAndEntradaIsNotNullAndSalidaIsNull(id, "RESIDENTE"))
            throw new IllegalArgumentException("El residente ya tiene una entrada sin salida.");
        var acceso = new Acceso();
        acceso.alquiler = alquiler;
        acceso.persona = alquiler.cuenta.nombre;
        acceso.tipo = "RESIDENTE";
        acceso.entrada = LocalDateTime.now();
        acceso.registradoPor = operador;
        accesos.save(acceso);
    }

    public void salida(Long id, String operador) {
        var acceso = obtener(id);
        if (acceso.entrada == null || acceso.salida != null)
            throw new IllegalArgumentException("No hay una entrada pendiente de salida.");
        acceso.salida = LocalDateTime.now();
        acceso.salidaPor = operador;
    }

    private Acceso obtener(Long id) {
        return accesos.bloquear(id).orElseThrow(() -> new IllegalArgumentException("Acceso inexistente."));
    }
}
