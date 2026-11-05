package mx.condominios;

import mx.condominios.entities.Alquiler;
import mx.condominios.entities.Cuenta;
import mx.condominios.repositories.RepositorioAccesos;
import mx.condominios.repositories.RepositorioAlquileres;
import mx.condominios.repositories.RepositorioCuentas;
import mx.condominios.businessLayer.ServicioAccesos;
import mx.condominios.businessLayer.ServicioAlquileres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:accesos;DB_CLOSE_DELAY=-1",
        "app.administrador.contrasena=" })
@AutoConfigureMockMvc
@Transactional
class PruebasAccesos {
    @Autowired
    MockMvc mvc;
    @Autowired
    RepositorioCuentas cuentas;
    @Autowired
    RepositorioAlquileres alquileres;
    @Autowired
    RepositorioAccesos accesos;
    @Autowired
    ServicioAlquileres alquiler;
    @Autowired
    ServicioAccesos servicio;

    private Alquiler preparar() {
        var cuenta = new Cuenta();
        cuenta.correo = "acceso@example.com";
        cuenta.nombre = "Residente Accesos";
        cuenta.contrasena = "sin-login";
        cuenta.rol = "USUARIO";
        cuentas.save(cuenta);
        alquiler.alquilar(4L, cuenta.correo, LocalDate.now());
        return alquileres.buscarPorCorreoDeCuenta(cuenta.correo).getFirst();
    }

    @Test
    void cicloCompletoYPantallas() throws Exception {
        var r = preparar();
        mvc.perform(post("/mis-visitas").with(user(r.cuenta.correo).roles("USUARIO")).with(csrf())
                .param("alquilerId", r.id.toString()).param("nombre", "Visitante Prueba")
                .param("fecha", LocalDate.now().toString()))
                .andExpect(redirectedUrl("/mis-visitas"));
        var visita = accesos.findAll().getFirst();
        mvc.perform(get("/mis-visitas").with(user(r.cuenta.correo).roles("USUARIO")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(visita.codigo)));
        mvc.perform(get("/mis-visitas").with(user("otro@example.com").roles("USUARIO")))
                .andExpect(status().isOk()).andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(visita.codigo))));
        mvc.perform(post("/administracion/accesos/visita").with(user("operador").roles("ADMINISTRADOR")).with(csrf())
                .param("codigo", visita.codigo)).andExpect(redirectedUrl("/administracion/accesos"));
        assertThat(visita.entrada).isNotNull();
        assertThatThrownBy(() -> servicio.entradaVisita(visita.codigo, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.cancelar(visita.id, r.cuenta.correo))
                .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(post("/administracion/accesos/" + visita.id + "/salida")
                .with(user("operador").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/administracion/accesos"));
        assertThat(visita.salida).isNotNull();
        assertThat(visita.salidaPor).isEqualTo("operador");
        assertThatThrownBy(() -> servicio.salida(visita.id, "operador")).isInstanceOf(IllegalArgumentException.class);
        servicio.entradaResidente(r.id, "operador");
        assertThatThrownBy(() -> servicio.entradaResidente(r.id, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/administracion/accesos").with(user("operador").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void reportePdfDeAccesos() throws Exception {
        var r = preparar();
        servicio.autorizar(r.id, r.cuenta.correo, "Visitante Reporte", LocalDate.now());
        servicio.entradaResidente(r.id, "operador@example.com");
        var respuesta = mvc.perform(get("/administracion/accesos/reporte.pdf")
                .with(user("operador").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=accesos.pdf"))
                .andReturn().getResponse();
        var lector = new com.lowagie.text.pdf.PdfReader(respuesta.getContentAsByteArray());
        try {
            String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(lector).getTextFromPage(1);
            assertThat(texto).contains("Reporte de accesos", "Visitante Reporte", "Residente Accesos",
                    "Autorizado", "Dentro", "operador@example.com");
        } finally {
            lector.close();
        }
    }

    @Test
    void reporteVacioYPermisos() throws Exception {
        mvc.perform(get("/administracion/accesos/reporte.pdf")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/administracion/accesos/reporte.pdf").with(user("residente").roles("USUARIO")))
                .andExpect(status().isForbidden());
        var respuesta = mvc.perform(get("/administracion/accesos/reporte.pdf")
                .with(user("operador").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andReturn().getResponse();
        var lector = new com.lowagie.text.pdf.PdfReader(respuesta.getContentAsByteArray());
        try {
            assertThat(new com.lowagie.text.pdf.parser.PdfTextExtractor(lector).getTextFromPage(1))
                    .contains("Total de registros: 0", "Todavía no hay accesos ni visitas registrados.");
        } finally {
            lector.close();
        }
    }

    @Test
    void autorizacionesInvalidas() {
        var r = preparar();
        assertThatThrownBy(() -> servicio.autorizar(r.id, "ajeno", "Visita", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.autorizar(r.id, r.cuenta.correo, " ", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.autorizar(r.id, r.cuenta.correo, "Visita", LocalDate.now().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        var visita = servicio.autorizar(r.id, r.cuenta.correo, "Visita", LocalDate.now());
        assertThatThrownBy(() -> servicio.cancelar(visita.id, "ajeno")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.salida(visita.id, "operador")).isInstanceOf(IllegalArgumentException.class);
        servicio.cancelar(visita.id, r.cuenta.correo);
        assertThatThrownBy(() -> servicio.entradaVisita(visita.codigo, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        var futura = servicio.autorizar(r.id, r.cuenta.correo, "Futura", LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> servicio.entradaVisita(futura.codigo, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        futura.fechaVisita = LocalDate.now().minusDays(1);
        assertThatThrownBy(() -> servicio.entradaVisita(futura.codigo, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.entradaVisita("inexistente", "operador"))
                .isInstanceOf(IllegalArgumentException.class);
        r.fechaInicio = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> servicio.entradaResidente(r.id, "operador"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void permisosYCsrf() throws Exception {
        mvc.perform(get("/administracion/accesos")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/administracion/accesos").with(user("residente").roles("USUARIO")))
                .andExpect(status().isForbidden());
        for (String ruta : new String[] { "/administracion/accesos/visita", "/administracion/accesos/residente",
                "/administracion/accesos/1/salida" }) {
            mvc.perform(post(ruta).with(user("residente").roles("USUARIO")).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post(ruta).with(user("operador").roles("ADMINISTRADOR"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/mis-visitas").with(user("residente").roles("USUARIO"))).andExpect(status().isForbidden());
        mvc.perform(get("/mis-visitas").with(user("operador").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }
}
