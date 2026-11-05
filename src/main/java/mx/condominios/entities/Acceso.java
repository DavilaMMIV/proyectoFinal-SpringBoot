package mx.condominios.entities;

import jakarta.persistence.*;
import java.time.*;

@Entity
public class Acceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false)
    public Alquiler alquiler;
    @Column(nullable = false, length = 120)
    public String persona;
    @Column(nullable = false)
    public String tipo;
    @Column(unique = true)
    public String codigo;
    public LocalDate fechaVisita;
    public boolean cancelado;
    public LocalDateTime entrada;
    public LocalDateTime salida;
    public String registradoPor;
    public String salidaPor;

    public Long getId() {
        return id;
    }

    public Alquiler getAlquiler() {
        return alquiler;
    }

    public String getPersona() {
        return persona;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    public boolean isCancelado() {
        return cancelado;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSalida() {
        return salida;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public String getSalidaPor() {
        return salidaPor;
    }

    public String getEstado() {
        if (cancelado)
            return "Cancelado";
        if (salida != null)
            return "Salida registrada";
        if (entrada != null)
            return "Dentro";
        if (fechaVisita != null && fechaVisita.isBefore(LocalDate.now()))
            return "Vencido";
        return "Autorizado";
    }
}
