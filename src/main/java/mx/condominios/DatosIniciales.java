package mx.condominios;

import mx.condominios.entities.Condominio;
import mx.condominios.entities.Cuenta;
import mx.condominios.repositories.RepositorioCondominios;
import mx.condominios.repositories.RepositorioCuentas;

import org.springframework.context.annotation.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatosIniciales {
    @Bean
    CommandLineRunner inicializarDatos(RepositorioCuentas cuentas, RepositorioCondominios condominios,
            PasswordEncoder codificador, @Value("${app.administrador.correo}") String correo,
            @Value("${app.administrador.contrasena}") String contrasena) {
        return argumentos -> {
            if (!contrasena.isBlank() && cuentas.buscarPorCorreo(correo.toLowerCase(java.util.Locale.ROOT)).isEmpty()) {
                if (contrasena.length() < 12
                        || contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                    throw new IllegalArgumentException(
                            "ADMIN_CONTRASENA debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8");
                var a = new Cuenta();
                a.correo = correo.toLowerCase(java.util.Locale.ROOT);
                a.contrasena = codificador.encode(contrasena);
                a.nombre = "Administración";
                a.rol = "ADMINISTRADOR";
                cuentas.save(a);
            }
            if (condominios.count() == 0) {
                String[] nombres = { "Olivo 101", "Encino 204", "Cedro 302", "Sauce 105", "Magnolia 401",
                        "Laurel 202" };
                for (int i = 0; i < nombres.length; i++) {
                    var c = new Condominio();
                    c.nombre = nombres[i];
                    c.ubicacion = i % 2 == 0 ? "Torre Jardín · Vista al parque" : "Torre Central · Vista a la ciudad";
                    c.recamaras = 2 + i % 2;
                    c.area = 85 + i * 12;
                    c.precio = 8500 + i * 1500;
                    condominios.save(c);
                }
            }
        };
    }
}
