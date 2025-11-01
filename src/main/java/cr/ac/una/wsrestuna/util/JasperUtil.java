// src/main/java/cr/ac/una/wsrestuna/util/JasperUtil.java
package cr.ac.una.wsrestuna.util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class JasperUtil {

    private JasperUtil() {}

    // ==================== MAPS ====================
    /**
     * Genera PDF a partir de una colección de Maps (key = nombre de campo).
     * @param jrxmlOnClasspath ruta al .jrxml en el classpath (ej. "/reports/facturas.jrxml")
     * @param data colección de filas. Acepta Collection<Map<String, ?>>.
     * @param params parámetros opcionales del reporte (puede ser null)
     */
    public static byte[] renderPdfFromMaps(
            String jrxmlOnClasspath,
            Collection<? extends Map<String, ?>> data,
            Map<String, Object> params
    ) {
        Collection<? extends Map<String, ?>> safeData =
                (data == null) ? Collections.<Map<String, ?>>emptyList() : data;

        JRMapCollectionDataSource ds =
                new JRMapCollectionDataSource((Collection<Map<String, ?>>) safeData);

        return compileFillExport(jrxmlOnClasspath, ds, params);
    }

    // ==================== BEANS ====================
    /**
     * Genera PDF a partir de una colección de beans (getters como campos).
     */
    public static byte[] renderPdfFromBeans(
            String jrxmlOnClasspath,
            Collection<?> beans,
            Map<String, Object> params
    ) {
        JRBeanCollectionDataSource ds =
                new JRBeanCollectionDataSource(beans == null ? Collections.emptyList() : beans);

        return compileFillExport(jrxmlOnClasspath, ds, params);
    }

    // ==================== CORE ====================
    private static byte[] compileFillExport(
            String jrxmlOnClasspath,
            JRDataSource dataSource,
            Map<String, Object> params
    ) {
        try (InputStream in = JasperUtil.class.getResourceAsStream(jrxmlOnClasspath)) {
            if (in == null) {
                throw new IllegalArgumentException("Plantilla no encontrada en classpath: " + jrxmlOnClasspath);
            }
            JasperReport report = JasperCompileManager.compileReport(in);

            // MUTABLE siempre (Jasper le inyecta/ajusta parámetros)
            Map<String, Object> mutableParams = new HashMap<>();
            if (params != null) mutableParams.putAll(params);

            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    mutableParams,
                    dataSource
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF con plantilla " + jrxmlOnClasspath + ": " + e.getMessage(), e);
        }
    }
}