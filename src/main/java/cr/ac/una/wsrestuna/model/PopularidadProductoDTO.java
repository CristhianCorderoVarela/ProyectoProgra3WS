package cr.ac.una.wsrestuna.model;

import java.math.BigDecimal;

public class PopularidadProductoDTO {
    private Long productoId;
    private String nombreProducto;
    private String nombreCorto;
    private String grupoNombre;
    private Long cantidadVentas;
    private BigDecimal ingresoTotal;
    /** % del total del período (0..100 con 2 decimales) */
    private Double porcentajeVentas;

    public PopularidadProductoDTO() {}

    public PopularidadProductoDTO(Long productoId, String nombreProducto, String nombreCorto,
                                  String grupoNombre, Long cantidadVentas, BigDecimal ingresoTotal) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.nombreCorto = nombreCorto;
        this.grupoNombre = grupoNombre;
        this.cantidadVentas = cantidadVentas;
        this.ingresoTotal = ingresoTotal;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getNombreCorto() { return nombreCorto; }
    public void setNombreCorto(String nombreCorto) { this.nombreCorto = nombreCorto; }

    public String getGrupoNombre() { return grupoNombre; }
    public void setGrupoNombre(String grupoNombre) { this.grupoNombre = grupoNombre; }

    public Long getCantidadVentas() { return cantidadVentas; }
    public void setCantidadVentas(Long cantidadVentas) { this.cantidadVentas = cantidadVentas; }

    public BigDecimal getIngresoTotal() { return ingresoTotal; }
    public void setIngresoTotal(BigDecimal ingresoTotal) { this.ingresoTotal = ingresoTotal; }

    public Double getPorcentajeVentas() { return porcentajeVentas; }
    public void setPorcentajeVentas(Double porcentajeVentas) { this.porcentajeVentas = porcentajeVentas; }
}
