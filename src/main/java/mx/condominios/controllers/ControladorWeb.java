package mx.condominios.controllers;

import mx.condominios.entities.Alquiler;
import mx.condominios.entities.Condominio;
import mx.condominios.entities.Cuenta;
import mx.condominios.dtos.Registro;
import mx.condominios.repositories.RepositorioAlquileres;
import mx.condominios.repositories.RepositorioCondominios;
import mx.condominios.repositories.RepositorioCuentas;
import mx.condominios.businessLayer.ServicioAlquileres;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

@Controller
public class ControladorWeb {
    private final RepositorioCuentas cuentas;
    private final RepositorioCondominios condominios;
    private final RepositorioAlquileres alquileres;
    private final ServicioAlquileres servicio;
    private final PasswordEncoder codificador;

    public ControladorWeb(RepositorioCuentas a, RepositorioCondominios c, RepositorioAlquileres r, ServicioAlquileres s,
            PasswordEncoder p) {
        cuentas = a;
        condominios = c;
        alquileres = r;
        servicio = s;
        codificador = p;
    }

    @ModelAttribute
    void navegacion(Authentication a, Model m) {
        m.addAttribute("sesionIniciada", a != null);
        m.addAttribute("esAdministrador",
                a != null && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMINISTRADOR")));
    }

    @GetMapping("/")
    String inicio(Model m) {
        m.addAttribute("condominios", condominios.findAll());
        m.addAttribute("ocupados", alquileres.findAll().stream().map(r -> r.condominio.id).toList());
        return "inicio";
    }

    @GetMapping("/iniciar-sesion")
    String iniciarSesion() {
        return "iniciar-sesion";
    }

    @GetMapping("/registro")
    String registrar(Model m) {
        m.addAttribute("registro", new Registro());
        return "registro";
    }

    @PostMapping("/registro")
    String registrar(@Valid @ModelAttribute("registro") Registro formulario, BindingResult errores, Model m) {
        if (errores.hasErrors()) {
            m.addAttribute("error",
                    "Revisa los datos: nombre completo, correo válido, teléfono de 10 a 20 caracteres y contraseña de 12 a 72 caracteres.");
            return "registro";
        }
        String correo = formulario.correo.trim().toLowerCase(java.util.Locale.ROOT);
        var a = new Cuenta();
        a.nombre = formulario.nombre.trim();
        a.correo = correo;
        a.telefono = formulario.telefono;
        a.contrasena = codificador.encode(formulario.contrasena);
        a.rol = "USUARIO";
        try {
            cuentas.saveAndFlush(a);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            m.addAttribute("error", "Ese correo ya está registrado.");
            return "registro";
        }
        return "redirect:/iniciar-sesion?registrado";
    }

    @GetMapping("/panel")
    String panel(Authentication a) {
        return a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMINISTRADOR"))
                ? "redirect:/administracion"
                : "redirect:/mis-alquileres";
    }

    @GetMapping("/mis-alquileres")
    String misAlquileres(Authentication a, Model m) {
        m.addAttribute("alquileres", alquileres.buscarPorCorreoDeCuenta(a.getName()));
        return "mis-alquileres";
    }

    @GetMapping("/alquilar/{id}")
    String formulario(@PathVariable Long id, Model m) {
        var c = condominios.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
        m.addAttribute("condominio", c);
        m.addAttribute("hoy", LocalDate.now());
        return "alquilar";
    }

    @PostMapping("/alquilar/{id}")
    String alquilar(@PathVariable Long id, @RequestParam String fechaInicio, Authentication a,
            RedirectAttributes mensajes) {
        try {
            servicio.alquilar(id, a.getName(), LocalDate.parse(fechaInicio));
            mensajes.addFlashAttribute("exito", "Alquiler registrado. Este es tu folio de identificación.");
            return "redirect:/mis-alquileres";
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            mensajes.addFlashAttribute("error",
                    e instanceof java.time.format.DateTimeParseException ? "La fecha no es válida." : e.getMessage());
            return "redirect:/alquilar/" + id;
        }
    }

    @GetMapping("/administracion")
    String administracion(Model m) {
        var lista = alquileres.findAll();
        m.addAttribute("alquileres", lista);
        m.addAttribute("total", condominios.count());
        m.addAttribute("ocupados", lista.size());
        m.addAttribute("ingresos", lista.stream().mapToInt(r -> r.condominio.precio).sum());
        return "administracion";
    }

    @GetMapping("/administracion/reporte.pdf")
    ResponseEntity<byte[]> generarReportePdf() {
        var salida = new ByteArrayOutputStream();
        var documento = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(documento, salida);
        documento.open();
        documento.add(
                new Paragraph("HABITA | Reporte de residentes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        documento.add(new Paragraph("Fecha: " + LocalDate.now() + " | Importes mensuales en MXN"));
        documento.add(new Paragraph(" "));
        var tabla = new PdfPTable(new float[] { 2, 3, 3, 2, 4, 2, 2 });
        tabla.setWidthPercentage(100);
        for (String h : new String[] { "Condominio", "Residente", "Correo", "Teléfono", "Folio", "Inicio", "Renta" })
            tabla.addCell(h);
        for (var r : alquileres.findAll()) {
            for (String s : new String[] { r.condominio.nombre, r.cuenta.nombre, r.cuenta.correo, r.cuenta.telefono,
                    r.folio, r.fechaInicio.toString(), "$" + r.condominio.precio })
                tabla.addCell(new Phrase(s, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        }
        documento.add(tabla);
        documento.close();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residentes.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(salida.toByteArray());
    }
}
