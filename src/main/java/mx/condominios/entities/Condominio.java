package mx.condominios.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "condo")
public class Condominio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "name")
    public String nombre;
    @Column(name = "location")
    public String ubicacion;
    @Column(name = "bedrooms")
    public int recamaras;
    public int area;
    @Column(name = "price")
    public int precio;

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public int getRecamaras() {
        return recamaras;
    }

    public int getArea() {
        return area;
    }

    public int getPrecio() {
        return precio;
    }
}
