package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.util.JasperUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class ReportesPdfService {

    @Inject
    ReportesService reportesService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] facturasPdf(LocalDate desde, LocalDate hasta, String usuario, String estado) {
        // Antes: reportesService.listarFacturas(...)
        List<Map<String,Object>> data = reportesService.facturas(desde, hasta, usuario, estado);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Listado de Facturas");
        params.put("P_RANGO",
            (desde==null? "" : desde.format(DF)) + "  →  " + (hasta==null? "" : hasta.format(DF)));

        return JasperUtil.renderPdfFromMaps("/reports/facturas.jrxml", data, params);
    }

    public byte[] productosTopPdf(LocalDate desde, LocalDate hasta, String grupo, Integer top) {
        // Antes: reportesService.listarProductosTop(...)
        List<Map<String,Object>> data = reportesService.productosTop(desde, hasta, grupo, top);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Productos más vendidos");
        params.put("P_RANGO",
            (desde==null? "" : desde.format(DF)) + "  →  " + (hasta==null? "" : hasta.format(DF)));

        return JasperUtil.renderPdfFromMaps("/reports/productos_top.jrxml", data, params);
    }

    public byte[] cierreCajaPdf(LocalDate fecha, String usuario) {
        // Antes: reportesService.listarCierres(...)
        List<Map<String,Object>> data = reportesService.cierres(fecha, usuario);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Cierre de Caja");
        params.put("P_FECHA",  fecha==null? "" : fecha.format(DF));
        params.put("P_CAJERO", usuario==null? "" : usuario);

        // Horizontal para la ventana de Reportes:
        return JasperUtil.renderPdfFromMaps("/reports/CierreCajaResumen_Landscape.jrxml", data, params);
        // Vertical (si prefieres):
        // return JasperUtil.renderPdfFromMaps("/reports/Cierres.jrxml", data, params);
    }
    
    
    
    public byte[] cierreByIdPdf(Long cierreId) {
    var data = reportesService.cierreById(cierreId);
    @SuppressWarnings("unchecked")
    var rows = (List<Map<String,Object>>) data.get("movimientos");
    @SuppressWarnings("unchecked")
    var head = (Map<String,Object>) data.get("cierre");

    Map<String,Object> p = new HashMap<>();
    p.put("P_TITULO", "Cierre de Caja (Detalle)");
    p.put("P_CIERRE_ID", cierreId);
    p.put("P_CAJERO", String.valueOf(head.getOrDefault("usuarioNombre", "")));
    p.put("P_APERTURA", String.valueOf(head.getOrDefault("apertura", "")));
    p.put("P_CIERRE", String.valueOf(head.getOrDefault("cierre", "")));
    p.put("P_EF_SIS", head.get("efectivoSistema"));
    p.put("P_TJ_SIS", head.get("tarjetaSistema"));
    p.put("P_EF_DECL", head.get("efectivoDecl"));
    p.put("P_TJ_DECL", head.get("tarjetaDecl"));
    p.put("P_DIF_EF", head.get("difEfectivo"));
    p.put("P_DIF_TJ", head.get("difTarjeta"));

    return JasperUtil.renderPdfFromMaps("/reports/cierres.jrxml", rows, p);
}
}