package cr.ac.una.wsrestuna.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@NamedQueries({
    @NamedQuery(name = "Producto.findAll", query = "SELECT p FROM Producto p"),
    @NamedQuery(name = "Producto.findActivos", query = "SELECT p FROM Producto p WHERE p.estado = 'A'"),
    @NamedQuery(name = "Producto.findByGrupo",
            query = "SELECT p FROM Producto p WHERE p.grupo.id = :grupoId AND p.estado = 'A'"),
    @NamedQuery(name = "Producto.findMenuRapido",
            query = "SELECT p FROM Producto p WHERE p.menuRapido = 'S' AND p.estado = 'A' ORDER BY p.totalVentas DESC"),
    @NamedQuery(name = "Producto.findMenuRapidoByGrupo",
            query = "SELECT p FROM Producto p WHERE p.grupo.id = :grupoId AND p.menuRapido = 'S' AND p.estado = 'A' ORDER BY p.totalVentas DESC"),
    @NamedQuery(name = "Producto.findMasVendidos",
            query = "SELECT p FROM Producto p WHERE p.estado = 'A' ORDER BY p.totalVentas DESC")
})
public class Producto implements Serializable {

    private static final long serialVersionUID = 1L;
    
    
    
    
    

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "seq_producto", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    @JsonbTransient  // ⭐ Evita serializar el objeto completo
    private GrupoProducto grupo;

    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nombre_corto", nullable = false, length = 50)
    private String nombreCorto;

    
    
    
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "menu_rapido", nullable = false, length = 1)
    private String menuRapido = "N";

    @Column(name = "total_ventas")
    private Long totalVentas = 0L;

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A";

    @Version
    @Column(name = "version")
    private Long version;

    // ⭐ NUEVO: Campo transient para exponer el grupoId en el JSON
    @Transient
    private Long grupoId;

    public Producto() {
        this.estado = "A";
        this.menuRapido = "N";
        this.totalVentas = 0L;
        this.precio = BigDecimal.ZERO;
    }

    public Producto(Long id) {
        this();
        this.id = id;
    }

    // ==================== Getters y Setters ====================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GrupoProducto getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoProducto grupo) {
        this.grupo = grupo;
        // Sincronizar grupoId cuando se setea el grupo
        if (grupo != null) {
            this.grupoId = grupo.getId();
        }
    }

    /**
     * ⭐ CRÍTICO: Getter que calcula el grupoId dinámicamente Se llamará
     * automáticamente durante la serialización JSON
     */
    public Long getGrupoId() {
        if (grupoId != null) {
            return grupoId;
        }
        // Fallback: obtener desde el objeto grupo si está cargado
        if (grupo != null) {
            return grupo.getId();
        }
        return null;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreCorto() {
        return nombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.nombreCorto = nombreCorto;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getMenuRapido() {
        return menuRapido;
    }

    public void setMenuRapido(String menuRapido) {
        this.menuRapido = menuRapido;
    }

    public Long getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(Long totalVentas) {
        this.totalVentas = totalVentas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // ==================== Métodos auxiliares ====================
    public boolean isActivo() {
        return "A".equals(this.estado);
    }

    public boolean isMenuRapido() {
        return "S".equals(this.menuRapido);
    }

    public void incrementarVentas() {
        if (this.totalVentas == null) {
            this.totalVentas = 0L;
        }
        this.totalVentas++;

        if (this.grupo != null) {
            this.grupo.incrementarVentas();
        }
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Producto)) {
            return false;
        }
        Producto other = (Producto) obj;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Producto[id=" + id + ", nombre=" + nombre + ", grupoId=" + getGrupoId() + "]";
    }
}
