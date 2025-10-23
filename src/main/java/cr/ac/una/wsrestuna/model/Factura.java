/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factura")
@NamedQueries({
    @NamedQuery(name = "Factura.findAll", query = "SELECT f FROM Factura f"),
    @NamedQuery(name = "Factura.findByFecha", 
                query = "SELECT f FROM Factura f WHERE f.fechaHora BETWEEN :fechaInicio AND :fechaFin ORDER BY f.fechaHora DESC"),
    @NamedQuery(name = "Factura.findByUsuario", query = "SELECT f FROM Factura f WHERE f.usuario.id = :usuarioId"),
    @NamedQuery(name = "Factura.findByCierre", query = "SELECT f FROM Factura f WHERE f.cierreCaja.id = :cierreId"),
    @NamedQuery(name = "Factura.findByCliente", query = "SELECT f FROM Factura f WHERE f.cliente.id = :clienteId")
})
public class Factura implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_seq")
    @SequenceGenerator(name = "factura_seq", sequenceName = "seq_factura", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cierre_caja_id")
    private CierreCaja cierreCaja;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0")
    @Column(name = "impuesto_venta", precision = 10, scale = 2)
    private BigDecimal impuestoVenta = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "impuesto_servicio", precision = 10, scale = 2)
    private BigDecimal impuestoServicio = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "descuento", precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @DecimalMin(value = "0.0")
    @Column(name = "monto_efectivo", precision = 10, scale = 2)
    private BigDecimal montoEfectivo = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "monto_tarjeta", precision = 10, scale = 2)
    private BigDecimal montoTarjeta = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "vuelto", precision = 10, scale = 2)
    private BigDecimal vuelto = BigDecimal.ZERO;

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A"; // A=Activa, C=Cancelada

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleFactura> detalles;

    public Factura() {
        this.estado = "A";
        this.fechaHora = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.detalles = new ArrayList<>();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Orden getOrden() { return orden; }
    public void setOrden(Orden orden) { this.orden = orden; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public CierreCaja getCierreCaja() { return cierreCaja; }
    public void setCierreCaja(CierreCaja cierreCaja) { this.cierreCaja = cierreCaja; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getImpuestoVenta() { return impuestoVenta; }
    public void setImpuestoVenta(BigDecimal impuestoVenta) { this.impuestoVenta = impuestoVenta; }
    public BigDecimal getImpuestoServicio() { return impuestoServicio; }
    public void setImpuestoServicio(BigDecimal impuestoServicio) { this.impuestoServicio = impuestoServicio; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getMontoEfectivo() { return montoEfectivo; }
    public void setMontoEfectivo(BigDecimal montoEfectivo) { this.montoEfectivo = montoEfectivo; }
    public BigDecimal getMontoTarjeta() { return montoTarjeta; }
    public void setMontoTarjeta(BigDecimal montoTarjeta) { this.montoTarjeta = montoTarjeta; }
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal vuelto) { this.vuelto = vuelto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFactura> detalles) { this.detalles = detalles; }

    public void addDetalle(DetalleFactura detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public boolean isActiva() { return "A".equals(this.estado); }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Factura)) return false;
        Factura other = (Factura) obj;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }
}