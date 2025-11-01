// src/main/java/cr/ac/una/wsrestuna/service/ReportesPdfService.java
package cr.ac.una.wsrestuna.service;

import jakarta.ejb.Stateless;
import java.time.LocalDate;

@Stateless
public class ReportesPdfService {

    public byte[] facturasPdf(LocalDate desde, LocalDate hasta, String usuario, String estado) {
        // TODO: genera y devuelve el PDF (Jasper) en bytes
        //       usa tus JRXML y tu ReportesService para traer datos
        return new byte[0];
    }

    public byte[] productosTopPdf(LocalDate desde, LocalDate hasta, String grupo, Integer top) {
        // TODO
        return new byte[0];
    }

    public byte[] cierreCajaPdf(LocalDate fecha, String usuario) {
        // TODO
        return new byte[0];
    }
}