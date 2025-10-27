package cr.ac.una.wsrestuna.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una orden/comanda en el restaurante.
 */
@Entity
@Table(name = "orden")
@NamedQueries({
    @NamedQuery(name = "Orden.findAll",
            query = "SELECT o FROM Orden o"),
    @NamedQuery(name = "Orden.findAbiertas",
            query = "SELECT o FROM Orden o WHERE o.estado = 'ABIERTA'"),
    @NamedQuery(name = "Orden.findByMesa",
            query = "SELECT o FROM Orden o WHERE o.mesa.id = :mesaId AND o.estado = 'ABIERTA'"),
    @NamedQuery(name = "Orden.findByUsuario",
            query = "SELECT o FROM Orden o WHERE o.usuario.id = :usuarioId"),
    @NamedQuery(name = "Orden.findByFecha",
            query = "SELECT o FROM Orden o WHERE o.fechaHora BETWEEN :fechaInicio AND :fechaFin")
})
public class Orden implements Serializable {
    private static final long serialVersionUID = 1L;

    // ========================
    // CAMPOS PERSISTENTES (BD)
    // ========================

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orden_seq")
    @SequenceGenerator(name = "orden_seq", sequenceName = "seq_orden", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    // mesa_id puede ser NULL cuando es una orden de barra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = true)
    private Mesa mesa;

    // usuario_id NO puede ser NULL en la BD
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ABIERTA"; // ABIERTA, FACTURADA, CANCELADA

    @Size(max = 500)
    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrden> detalles;

    // ========================
    // CAMPOS TRANSIENT (NO BD)
    // ========================
    // Estos son IMPORTANTES:
    // El cliente (JavaFX) manda usuarioId y mesaId como números.
    // El backend los usa para setUsuario()/setMesa() ANTES de guardar.
    // JPA NO intenta persistirlos directo.

    @Transient
    private Long usuarioId;

    @Transient
    private Long mesaId;

    // ========================
    // CONSTRUCTOR
    // ========================

    public Orden() {
        this.estado = "ABIERTA";
        this.fechaHora = LocalDateTime.now();
        this.detalles = new ArrayList<>();
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

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<DetalleOrden> getDetalles() {
        return detalles;
    }

    /**
     * MUY IMPORTANTE:
     * cuando seteamos los detalles, también les seteamos el "orden = this"
     * para que la relación bidireccional quede consistente antes de persistir.
     */
    public void setDetalles(List<DetalleOrden> detalles) {
        this.detalles = detalles;
        if (this.detalles != null) {
            for (DetalleOrden d : this.detalles) {
                d.setOrden(this);
            }
        }
    }

    // ========================
    // GETTERS / SETTERS TRANSIENT
    // ========================

    public Long getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getMesaId() {
        return mesaId;
    }
    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
    }

    // ========================
    // HELPERS
    // ========================

    public void addDetalle(DetalleOrden detalle) {
        if (detalles == null) {
            detalles = new ArrayList<>();
        }
        detalles.add(detalle);
        detalle.setOrden(this);
    }

    public void removeDetalle(DetalleOrden detalle) {
        if (detalles != null) {
            detalles.remove(detalle);
        }
        if (detalle != null) {
            detalle.setOrden(null);
        }
    }

    public boolean isAbierta() {
        return "ABIERTA".equals(this.estado);
    }

    public boolean isFacturada() {
        return "FACTURADA".equals(this.estado);
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
        if (!(obj instanceof Orden)) return false;
        Orden other = (Orden) obj;
        return (this.id != null || other.id == null)
            && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Orden[id=" + id +
               ", estado=" + estado +
               ", mesa=" + (mesa != null ? mesa.getId() : "BARRA") +
               ", usuario=" + (usuario != null ? usuario.getId() : "null") +
               "]";
    }
}