package cr.ac.una.wsrestuna.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entidad que representa un detalle/línea de una orden.
 * ⭐ CORREGIDA: Ahora incluye productoId transient y maneja serialización JSON correctamente
 */
@Entity
@Table(name = "detalle_orden")
@NamedQueries({
    @NamedQuery(name = "DetalleOrden.findByOrden", 
                query = "SELECT d FROM DetalleOrden d WHERE d.orden.id = :ordenId")
})
public class DetalleOrden implements Serializable {
    private static final long serialVersionUID = 1L;

    // ========================
    // CAMPOS PERSISTENTES (BD)
    // ========================

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "detalle_orden_seq")
    @SequenceGenerator(name = "detalle_orden_seq", sequenceName = "seq_detalle_orden", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonbTransient  // ⭐ CRÍTICO: Evita referencia circular al serializar
    private Orden orden;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Version
    @Column(name = "version")
    private Long version;

    // ========================
    // CAMPO TRANSIENT (NO BD)
    // ========================
    // ⭐ ESTE ES EL CAMPO FALTANTE QUE CAUSA EL ERROR
    // El cliente envía solo el ID del producto, no el objeto completo
    // El servidor usa este ID para cargar la entidad Producto antes de persistir

    @Transient
    private Long productoId;

    // ========================
    // CONSTRUCTOR
    // ========================

    public DetalleOrden() {
        this.cantidad = 1;
        this.precioUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    // ========================
    // GETTERS / SETTERS BD
    // ========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Orden getOrden() {
        return orden;
    }

    public void setOrden(Orden orden) {
        this.orden = orden;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // ========================
    // GETTERS / SETTERS TRANSIENT
    // ========================

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    // ========================
    // HELPERS
    // ========================

    /**
     * Calcula el subtotal multiplicando precio unitario por cantidad
     */
    public void calcularSubtotal() {
        if (this.cantidad != null && this.precioUnitario != null) {
            this.subtotal = this.precioUnitario.multiply(BigDecimal.valueOf(this.cantidad));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    /**
     * Inicializa el precio desde el producto si existe
     */
    public void inicializarPrecio() {
        if (this.producto != null && this.producto.getPrecio() != null) {
            this.precioUnitario = this.producto.getPrecio();
            calcularSubtotal();
        }
    }

    // ========================
    // equals / hashCode
    // ========================

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DetalleOrden)) return false;
        DetalleOrden other = (DetalleOrden) obj;
        return (this.id != null || other.id == null) 
            && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "DetalleOrden[id=" + id +
               ", producto=" + (producto != null ? producto.getNombre() : productoId) +
               ", cantidad=" + cantidad +
               ", subtotal=" + subtotal +
               "]";
    }
}