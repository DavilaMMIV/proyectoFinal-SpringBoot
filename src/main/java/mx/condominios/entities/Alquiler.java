package mx.condominios.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rental")
public class Alquiler {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    public Cuenta cuenta;
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "condo_id", unique = true)
    public Condominio condominio;
    @Column(unique = true, nullable = false)
    public String folio;
    @Column(name = "start_date")
    public LocalDate fechaInicio;

    public Long getId() {
        return id;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public Condominio getCondominio() {
        return condominio;
    }

    public String getFolio() {
        return folio;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
}
