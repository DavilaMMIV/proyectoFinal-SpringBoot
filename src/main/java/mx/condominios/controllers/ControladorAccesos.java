package mx.condominios.controllers;

import mx.condominios.repositories.RepositorioAccesos;
import mx.condominios.repositories.RepositorioAlquileres;
import mx.condominios.businessLayer.ServicioAccesos;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import org.springframework.http.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

@Controller
public class ControladorAccesos {
    private final ServicioAccesos servicio;
    private final RepositorioAccesos accesos;
    private final RepositorioAlquileres alquileres;

    public ControladorAccesos(ServicioAccesos servicio, RepositorioAccesos accesos, RepositorioAlquileres alquileres) {
        this.servicio = servicio;
        this.accesos = accesos;
        this.alquileres = alquileres;
    }

    @GetMapping("/mis-visitas")
    String visitas(Authentication usuario, Model modelo) {
        modelo.addAttribute("alquileres", alquileres.buscarPorCorreoDeCuenta(usuario.getName()));
        modelo.addAttribute("accesos", accesos.findByAlquilerCuentaCorreoOrderByIdDesc(usuario.getName()));
        modelo.addAttribute("administrador", false);
        modelo.addAttribute("hoy", LocalDate.now());
        return "accesos";
    }

    @GetMapping("/administracion/accesos")
    String panel(Model modelo) {
        modelo.addAttribute("alquileres", alquileres.findAll());
        modelo.addAttribute("accesos", accesos.findAllByOrderByIdDesc());
        modelo.addAttribute("administrador", true);
        return "accesos";
    }

    @GetMapping("/administracion/accesos/reporte.pdf")
    ResponseEntity<byte[]> reportePdf() {
        var registros = accesos.findAllByOrderByIdDesc();
        var salida = new ByteArrayOutputStream();
        var documento = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(documento, salida);
        documento.open();
        documento.add(new Paragraph("HABITA | Reporte de accesos",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        documento.add(new Paragraph("Generado: " + formatearFecha(LocalDateTime.now())
                + " | Total de registros: " + registros.size()));
        documento.add(new Paragraph("Horarios en la zona horaria del servidor."));
        documento.add(new Paragraph(" "));
        var tabla = new PdfPTable(new float[] { 3, 2, 3, 2, 2, 3, 3, 3, 3 });
        tabla.setWidthPercentage(100);
        for (String titulo : new String[] { "Persona", "Tipo", "Condominio", "Fecha de visita", "Estado",
                "Entrada", "Salida", "Registró entrada", "Registró salida" }) {
            tabla.addCell(new Phrase(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        }
        tabla.setHeaderRows(1);
        for (var acceso : registros) {
            for (String valor : new String[] { acceso.persona, acceso.tipo, acceso.alquiler.condominio.nombre,
                    acceso.fechaVisita == null ? "—" : acceso.fechaVisita.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    acceso.getEstado(), formatearFecha(acceso.entrada), formatearFecha(acceso.salida),
                    acceso.registradoPor, acceso.salidaPor }) {
                tabla.addCell(new Phrase(valor == null ? "—" : valor,
                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
            }
        }
        if (registros.isEmpty()) {
            var celda = new PdfPCell(new Phrase("Todavía no hay accesos ni visitas registrados."));
            celda.setColspan(9);
            tabla.addCell(celda);
        }
        documento.add(tabla);
        documento.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=accesos.pdf")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PDF).body(salida.toByteArray());
    }

    private String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "—" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    @PostMapping("/mis-visitas")
    String autorizar(@RequestParam Long alquilerId, @RequestParam String nombre, @RequestParam String fecha,
            Authentication usuario, RedirectAttributes mensajes) {
        return ejecutar(() -> servicio.autorizar(alquilerId, usuario.getName(), nombre, LocalDate.parse(fecha)),
                mensajes, "/mis-visitas");
    }

    @PostMapping("/mis-visitas/{id}/cancelar")
    String cancelar(@PathVariable Long id, Authentication usuario, RedirectAttributes mensajes) {
        return ejecutar(() -> servicio.cancelar(id, usuario.getName()), mensajes, "/mis-visitas");
    }

    @PostMapping("/administracion/accesos/visita")
    String visita(@RequestParam String codigo, Authentication usuario, RedirectAttributes mensajes) {
        return ejecutar(() -> servicio.entradaVisita(codigo, usuario.getName()), mensajes, "/administracion/accesos");
    }

    @PostMapping("/administracion/accesos/residente")
    String residente(@RequestParam Long alquilerId, Authentication usuario, RedirectAttributes mensajes) {
        return ejecutar(() -> servicio.entradaResidente(alquilerId, usuario.getName()), mensajes,
                "/administracion/accesos");
    }

    @PostMapping("/administracion/accesos/{id}/salida")
    String salida(@PathVariable Long id, Authentication usuario, RedirectAttributes mensajes) {
        return ejecutar(() -> servicio.salida(id, usuario.getName()), mensajes, "/administracion/accesos");
    }

    private String ejecutar(Runnable operacion, RedirectAttributes mensajes, String destino) {
        try {
            operacion.run();
            mensajes.addFlashAttribute("exito", "Operación registrada correctamente.");
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            mensajes.addFlashAttribute("error",
                    e instanceof java.time.format.DateTimeParseException ? "Fecha inválida." : e.getMessage());
        }
        return "redirect:" + destino;
    }
}
