package cr.ac.una.wsrestuna.jsf;

import cr.ac.una.wsrestuna.model.PopularidadProductoDTO;
import cr.ac.una.wsrestuna.model.Producto;
import cr.ac.una.wsrestuna.service.ProductoService;
import cr.ac.una.wsrestuna.service.ReportesService;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Named("popularidadBean")
@RequestScoped
public class PopularidadBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @EJB
    private ReportesService reportesService;

    @EJB
    private ProductoService productoService;

    // Filtros
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Long grupoId; // null = todos
    private List<SelectItem> gruposUI; // <-- para evitar ConverterException

    // Datos y gráfico
    private List<PopularidadProductoDTO> productos = new ArrayList<>();
    private BarChartModel barModel;

    // Estadísticas
    private Long totalVentas = 0L;
    private BigDecimal totalIngresos = BigDecimal.ZERO;
    private int cantidadProductos = 0;
    private BigDecimal promedio = BigDecimal.ZERO;

    // Extras
    private PopularidadProductoDTO productoEstrellaSemana;
    private List<Producto> productosEnRiesgoSemana = new ArrayList<>();

    @PostConstruct
    public void init() {
        LocalDate hoy = LocalDate.now();
        this.fechaInicio = hoy.withDayOfMonth(1);
        this.fechaFin = hoy;
        cargarGruposParaUI();
        cargarDatos();
    }

    private void cargarGruposParaUI() {
        try {
            List<Producto> activos = productoService.findActivos();
            if (activos == null) activos = new ArrayList<>();

            // Mapa ordenado id->nombre
            Map<Long,String> mapa = new LinkedHashMap<>();
            for (Producto p : activos) {
                if (p != null && p.getGrupo() != null && p.getGrupo().getId() != null) {
                    mapa.putIfAbsent(p.getGrupo().getId(), p.getGrupo().getNombre());
                }
            }

            gruposUI = new ArrayList<>();
            gruposUI.add(new SelectItem(null, "Todos")); // selección nula

            for (Map.Entry<Long,String> e : mapa.entrySet()) {
                gruposUI.add(new SelectItem(e.getKey(), e.getValue())); // Long id
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            gruposUI = new ArrayList<>(List.of(new SelectItem(null, "Todos")));
        }
    }

    // ============ Carga principal ============
    public void cargarDatos() {
        try {
            // 1) Popularidad en el rango elegido
            productos = reportesService.popularidadProductos(fechaInicio, fechaFin, grupoId);
            if (productos == null) productos = new ArrayList<>();

            productos.sort(Comparator.comparingLong(PopularidadProductoDTO::getCantidadVentas).reversed());

            calcularEstadisticas();
            generarGraficoBarrasTop5();

            // 2) Estrella de la semana (últimos 7 días)
            LocalDate hoy = LocalDate.now();
            LocalDate hace7 = hoy.minusDays(6);
            List<PopularidadProductoDTO> semana = reportesService.popularidadProductos(hace7, hoy, grupoId);
            if (semana != null && !semana.isEmpty()) {
                semana.sort(Comparator.comparingLong(PopularidadProductoDTO::getCantidadVentas).reversed());
                productoEstrellaSemana = semana.get(0);
            } else {
                productoEstrellaSemana = null;
            }

            // 3) En riesgo: activos sin ventas en 7 días
            List<Producto> activos = productoService.findActivos();
            if (activos == null) activos = new ArrayList<>();
            if (grupoId != null) {
                activos = activos.stream()
                        .filter(p -> p.getGrupo() != null && grupoId.equals(p.getGrupo().getId()))
                        .collect(Collectors.toList());
            }
            Set<Long> vendidosSemana = (semana == null ? Set.of() :
                    semana.stream().map(PopularidadProductoDTO::getProductoId).collect(Collectors.toSet()));
            productosEnRiesgoSemana = activos.stream()
                    .filter(p -> p.getId() != null && !vendidosSemana.contains(p.getId()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            productos = new ArrayList<>();
            productoEstrellaSemana = null;
            productosEnRiesgoSemana = new ArrayList<>();
        }
    }

    private void calcularEstadisticas() {
        if (productos.isEmpty()) {
            totalVentas = 0L;
            totalIngresos = BigDecimal.ZERO;
            cantidadProductos = 0;
            promedio = BigDecimal.ZERO;
            return;
        }
        totalVentas = productos.stream().mapToLong(PopularidadProductoDTO::getCantidadVentas).sum();
        totalIngresos = productos.stream()
                .map(PopularidadProductoDTO::getIngresoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cantidadProductos = productos.size();
        promedio = (cantidadProductos > 0)
                ? totalIngresos.divide(BigDecimal.valueOf(cantidadProductos), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
    }

    private void generarGraficoBarrasTop5() {
        try {
            barModel = new BarChartModel();
            ChartData data = new ChartData();

            BarChartDataSet ds = new BarChartDataSet();
            ds.setLabel("Ventas");

            int limite = Math.min(5, productos.size());
            List<Number> valores = new ArrayList<>(limite);
            List<String> labels = new ArrayList<>(limite);
            List<String> bg = new ArrayList<>(limite);
            List<String> border = new ArrayList<>(limite);

            for (int i = 0; i < limite; i++) {
                PopularidadProductoDTO dto = productos.get(i);
                labels.add(dto.getNombreCorto() != null ? dto.getNombreCorto() : dto.getNombreProducto());
                valores.add(dto.getCantidadVentas());
                bg.add("rgba(102,126,234,0.8)");
                border.add("rgba(102,126,234,1)");
            }

            ds.setData(valores);
            ds.setBackgroundColor(bg);
            ds.setBorderColor(border);
            ds.setBorderWidth(2);

            data.addChartDataSet(ds);
            data.setLabels(labels);
            barModel.setData(data);

            BarChartOptions opts = new BarChartOptions();
            Title t = new Title();
            t.setDisplay(true);
            t.setText("Top 5");
            opts.setTitle(t);

            Legend lg = new Legend();
            lg.setDisplay(false);
            opts.setLegend(lg);

            barModel.setOptions(opts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Formatos cortos =====
    public String getTotalIngresosFormateado() {
        return totalIngresos != null ? String.format("₡%.2f", totalIngresos) : "₡0.00";
    }
    public String getPromedioFormateado() {
        return promedio != null ? String.format("₡%.2f", promedio) : "₡0.00";
    }

    // ===== Getters/Setters =====
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Long getGrupoId() { return grupoId; }
    public void setGrupoId(Long grupoId) { this.grupoId = grupoId; }

    public List<SelectItem> getGruposUI() { return gruposUI; }

    public List<PopularidadProductoDTO> getProductos() { return productos; }
    public BarChartModel getBarModel() { return barModel; }

    public Long getTotalVentas() { return totalVentas; }
    public BigDecimal getTotalIngresos() { return totalIngresos; }
    public int getCantidadProductos() { return cantidadProductos; }
    public BigDecimal getPromedio() { return promedio; }

    public PopularidadProductoDTO getProductoEstrellaSemana() { return productoEstrellaSemana; }
    public List<Producto> getProductosEnRiesgoSemana() { return productosEnRiesgoSemana; }
}
