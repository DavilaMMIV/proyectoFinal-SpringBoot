package mx.condominios.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "account")
public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "email", nullable = false, unique = true)
    public String correo;
    @Column(name = "password", nullable = false)
    public String contrasena;
    @Column(name = "name")
    public String nombre;
    @Column(name = "phone")
    public String telefono;
    @Column(name = "role")
    public String rol;

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }
}
