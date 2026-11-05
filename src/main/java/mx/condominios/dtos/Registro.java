package mx.condominios.dtos;

import jakarta.validation.constraints.*;

public class Registro {
    @NotBlank
    @Size(min = 5, max = 120)
    public String nombre;
    @NotBlank
    @Email
    @Size(max = 180)
    public String correo;
    @NotBlank
    @Pattern(regexp = "[+0-9 ()-]{10,20}")
    public String telefono;
    @NotBlank
    @Size(min = 12, max = 72)
    public String contrasena;

    @AssertTrue(message = "La contraseña debe ocupar como máximo 72 bytes UTF-8")
    public boolean isContrasenaDentroDelLimite() {
        return contrasena == null || contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String v) {
        nombre = v;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String v) {
        correo = v;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String v) {
        telefono = v;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String v) {
        contrasena = v;
    }
}
