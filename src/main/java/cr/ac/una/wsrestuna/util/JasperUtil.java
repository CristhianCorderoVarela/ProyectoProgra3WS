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

    public static byte[] renderPdfFromBeans(
            String jrxmlOnClasspath,
            Collection<?> beans,
            Map<String, Object> params
    ) {
        JRBeanCollectionDataSource ds =
                new JRBeanCollectionDataSource(beans == null ? Collections.emptyList() : beans);

        return compileFillExport(jrxmlOnClasspath, ds, params);
    }

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