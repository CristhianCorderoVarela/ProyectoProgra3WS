// src/main/java/cr/ac/una/wsrestuna/service/ReportesPdfService.java
package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.util.JasperUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

@Stateless
public class ReportesPdfService {

    @Inject
    ReportesService reportesService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Cast seguro para adaptar List<Map<String,Object>> a Collection<Map<String,?>> */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Collection<Map<String, ?>> wild(List<Map<String, Object>> in) {
        return (Collection) in;
    }

    // ================== PDFs LISTADOS ==================

    public byte[] facturasPdf(LocalDate desde, LocalDate hasta, String usuario, String estado) {
        List<Map<String,Object>> data = reportesService.facturas(desde, hasta, usuario, estado);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Listado de Facturas");
        params.put("P_RANGO",
                (desde==null? "" : desde.format(DF)) + "  →  " + (hasta==null? "" : hasta.format(DF)));

        return JasperUtil.renderPdfFromMaps("/reports/facturas.jrxml", wild(data), params);
    }

    public byte[] productosTopPdf(LocalDate desde, LocalDate hasta, String grupo, Integer top) {
        List<Map<String,Object>> data = reportesService.productosTop(desde, hasta, grupo, top);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Productos más vendidos");
        params.put("P_RANGO",
                (desde==null? "" : desde.format(DF)) + "  →  " + (hasta==null? "" : hasta.format(DF)));

        return JasperUtil.renderPdfFromMaps("/reports/productos_top.jrxml", wild(data), params);
    }

    public byte[] cierreCajaPdf(LocalDate fecha, String usuario) {
        List<Map<String,Object>> data = reportesService.cierres(fecha, usuario);

        Map<String,Object> params = new HashMap<>();
        params.put("P_TITULO", "Cierre de Caja");
        params.put("P_FECHA",  fecha==null? "" : fecha.format(DF));
        params.put("P_CAJERO", usuario==null? "" : usuario);

        return JasperUtil.renderPdfFromMaps("/reports/cierres.jrxml", wild(data), params);
    }

    private static java.io.InputStream openReport(String... candidates) {
    for (String c : candidates) {
        var is = ReportesPdfService.class.getResourceAsStream(c);
        if (is != null) return is;
    }
    throw new IllegalStateException("Plantilla no encontrada. Probé: " + java.util.Arrays.toString(candidates));
}

public byte[] cierreByIdPdf(Long id) {
    var data = reportesService.cierreById(id);
    @SuppressWarnings("unchecked") var cab = (java.util.Map<String,Object>) data.get("cierre");
    @SuppressWarnings("unchecked") var movimientos = (java.util.List<java.util.Map<String,Object>>) data.get("movimientos");

    var params = new java.util.HashMap<String,Object>();
    params.put("P_TITULO", "Cierre de Caja");
    params.put("CAB_ID",        cab.get("id"));
    params.put("CAB_ESTADO",    cab.get("estado"));
    params.put("CAB_APERTURA",  cab.get("apertura"));
    params.put("CAB_CIERRE",    cab.get("cierre"));
    params.put("CAB_USUARIO",   cab.get("usuarioNombre"));
    params.put("CAB_EF_SIS",    cab.get("efectivoSistema"));
    params.put("CAB_TJ_SIS",    cab.get("tarjetaSistema"));
    params.put("CAB_EF_DEC",    cab.get("efectivoDecl"));
    params.put("CAB_TJ_DEC",    cab.get("tarjetaDecl"));
    params.put("CAB_DIF_EF",    cab.get("difEfectivo"));
    params.put("CAB_DIF_TJ",    cab.get("difTarjeta"));
    params.put(net.sf.jasperreports.engine.JRParameter.REPORT_LOCALE, new java.util.Locale("es","CR"));
    params.put("DS_MOV", new net.sf.jasperreports.engine.data.JRMapCollectionDataSource((java.util.Collection) movimientos));

    try (var is = openReport(
            "/reports/CierreCajaDetalle.jrxml",   // 👈 tu carpeta real
            "/Reports/CierreCajaDetalle.jrxml",   // fallbacks por si hay cambios de mayúsculas
            "/reportes/CierreCajaDetalle.jrxml"
    )) {
        var jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(is);
        var jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, params, new net.sf.jasperreports.engine.JREmptyDataSource(1));
        return net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(jp);
    } catch (Exception e) {
        throw new RuntimeException("No se pudo generar PDF de cierre " + id + ": " + e.getMessage(), e);
    }
}

}