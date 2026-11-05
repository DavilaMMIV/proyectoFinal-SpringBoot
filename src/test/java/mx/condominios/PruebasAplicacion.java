package mx.condominios;

import mx.condominios.entities.Cuenta;
import mx.condominios.repositories.RepositorioAlquileres;
import mx.condominios.repositories.RepositorioCuentas;
import mx.condominios.businessLayer.ServicioAlquileres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.administrador.contrasena=" })
@AutoConfigureMockMvc
class PruebasAplicacion {
    @Autowired
    MockMvc mvc;
    @Autowired
    RepositorioCuentas cuentas;
    @Autowired
    RepositorioAlquileres alquileres;
    @Autowired
    ServicioAlquileres servicio;
    @Autowired
    PasswordEncoder codificador;

    @Test
    void paginasPublicasYRegistro() throws Exception {
        for (String ruta : new String[] { "/", "/iniciar-sesion", "/registro" })
            mvc.perform(get(ruta)).andExpect(status().isOk());
        mvc.perform(post("/registro").with(csrf()).param("nombre", "Ana López García").param("telefono", "5512345678")
                .param("correo", "ana@example.com").param("contrasena", "UnaClaveLarga123!"))
                .andExpect(redirectedUrl("/iniciar-sesion?registrado"));
        assertThat(cuentas.buscarPorCorreo("ana@example.com").orElseThrow().rol).isEqualTo("USUARIO");
        mvc.perform(post("/registro").param("nombre", "x")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioNoPuedeVerAdministracion() throws Exception {
        mvc.perform(get("/administracion")).andExpect(status().isForbidden());
        mvc.perform(get("/administracion/reporte.pdf")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeDescargarPdf() throws Exception {
        mvc.perform(get("/administracion")).andExpect(status().isOk());
        mvc.perform(get("/administracion/reporte.pdf")).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(resultado -> assertThat(new String(resultado.getResponse().getContentAsByteArray(), 0, 4))
                        .isEqualTo("%PDF"));
    }

    @Test
    void alquilerUnicoYFechasValidas() {
        var a = new Cuenta();
        a.correo = "resident@example.com";
        a.nombre = "Luis Pérez";
        a.telefono = "5512345678";
        a.contrasena = codificador.encode("password12345");
        a.rol = "USUARIO";
        cuentas.save(a);
        assertThatThrownBy(() -> servicio.alquilar(1L, a.correo, java.time.LocalDate.now().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        servicio.alquilar(1L, a.correo, java.time.LocalDate.now());
        assertThat(alquileres.buscarPorCorreoDeCuenta(a.correo)).hasSize(1);
        assertThat(alquileres.buscarPorCorreoDeCuenta(a.correo).getFirst().folio).startsWith("HAB-");
        assertThatThrownBy(() -> servicio.alquilar(1L, a.correo, java.time.LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "viewer@example.com", roles = "USUARIO")
    void paginasDeUsuarioDisponibles() throws Exception {
        mvc.perform(get("/mis-alquileres")).andExpect(status().isOk());
        mvc.perform(get("/alquilar/2")).andExpect(status().isOk());
        mvc.perform(get("/alquilar/99999")).andExpect(status().isNotFound());
    }

    @Test
    void cuentasAnterioresPuedenIniciarSesion() throws Exception {
        var cuenta = new Cuenta();
        cuenta.correo = "administrador-anterior@example.com";
        cuenta.nombre = "Administrador anterior";
        cuenta.contrasena = codificador.encode("ClaveAnterior123!");
        cuenta.rol = "ADMIN";
        cuentas.save(cuenta);
        var respuesta = mvc.perform(post("/iniciar-sesion").with(csrf()).param("correo", cuenta.correo)
                .param("contrasena", "ClaveAnterior123!")).andExpect(redirectedUrl("/panel")).andReturn();
        var sesion = (org.springframework.mock.web.MockHttpSession) respuesta.getRequest().getSession(false);
        mvc.perform(get("/administracion").session(sesion)).andExpect(status().isOk());
        mvc.perform(post("/cerrar-sesion").session(sesion).with(csrf())).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(username = "inquilino@example.com", roles = "USUARIO")
    void alquilerConCamposEnEspanol() throws Exception {
        var cuenta = new Cuenta();
        cuenta.correo = "inquilino@example.com";
        cuenta.nombre = "María Hernández";
        cuenta.telefono = "5512345678";
        cuenta.contrasena = codificador.encode("ClaveResidente123!");
        cuenta.rol = "USUARIO";
        cuentas.save(cuenta);
        mvc.perform(post("/alquilar/3").with(csrf()).param("fechaInicio", java.time.LocalDate.now().toString()))
                .andExpect(redirectedUrl("/mis-alquileres"));
        var alquiler = alquileres.buscarPorCorreoDeCuenta(cuenta.correo).getFirst();
        mvc.perform(get("/mis-alquileres")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(alquiler.folio)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(cuenta.nombre)));
    }

    @Test
    void anonimoNoPuedeVerResidentes() throws Exception {
        mvc.perform(get("/administracion")).andExpect(status().is3xxRedirection());
    }
}
