package mx.condominios;

import mx.condominios.repositories.RepositorioCuentas;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    PasswordEncoder codificador() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(RepositorioCuentas cuentas) {
        return correo -> {
            var a = cuentas.buscarPorCorreo(correo.toLowerCase(java.util.Locale.ROOT))
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            return User.withUsername(a.correo).password(a.contrasena).roles(switch (a.rol) {
                case "ADMIN" -> "ADMINISTRADOR";
                case "USER" -> "USUARIO";
                default -> a.rol;
            }).build();
        };
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(
                        a -> a.requestMatchers("/", "/iniciar-sesion", "/registro", "/estilos.css", "/estilos-login.css", "/imagenes/**", "/error")
                                .permitAll().requestMatchers("/administracion/**").hasRole("ADMINISTRADOR")
                                .requestMatchers("/alquilar/**", "/mis-alquileres", "/mis-visitas/**").hasRole("USUARIO").anyRequest()
                                .authenticated())
                .formLogin(f -> f.loginPage("/iniciar-sesion").usernameParameter("correo")
                        .passwordParameter("contrasena").defaultSuccessUrl("/panel", true).permitAll())
                .logout(l -> l.logoutUrl("/cerrar-sesion").logoutSuccessUrl("/")).build();
    }
}
