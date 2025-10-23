package cr.ac.una.wsrestuna.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa salones o secciones del restaurante
 * Tipo SALON: requiere diseño de mesas
 * Tipo BARRA: venta directa sin mesas
 * 
 * @author Tu Nombre
 */
@Entity
@Table(name = "salon")
@NamedQueries({
    @NamedQuery(name = "Salon.findAll", query = "SELECT s FROM Salon s"),
    @NamedQuery(name = "Salon.findActivos", query = "SELECT s FROM Salon s WHERE s.estado = 'A'"),
    @NamedQuery(name = "Salon.findByTipo", query = "SELECT s FROM Salon s WHERE s.tipo = :tipo AND s.estado = 'A'"),
    @NamedQuery(name = "Salon.findSalones", query = "SELECT s FROM Salon s WHERE s.tipo = 'SALON' AND s.estado = 'A'")
})
public class Salon implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "salon_seq")
    @SequenceGenerator(name = "salon_seq", sequenceName = "seq_salon", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // SALON, BARRA

    @Lob
    @Column(name = "imagen_mesa")
    private byte[] imagenMesa;

    @Size(max = 50)
    @Column(name = "tipo_imagen", length = 50)
    private String tipoImagen; // image/png, image/jpeg, etc.

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "cobra_servicio", nullable = false, length = 1)
    private String cobraServicio = "S"; // S=Sí, N=No

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A"; // A=Activo, I=Inactivo

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mesa> mesas;

    // Constructores
    public Salon() {
        this.estado = "A";
        this.cobraServicio = "S";
        this.mesas = new ArrayList<>();
    }

    public Salon(Long id) {
        this();
        this.id = id;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public byte[] getImagenMesa() {
        return imagenMesa;
    }

    public void setImagenMesa(byte[] imagenMesa) {
        this.imagenMesa = imagenMesa;
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public String getCobraServicio() {
        return cobraServicio;
    }

    public void setCobraServicio(String cobraServicio) {
        this.cobraServicio = cobraServicio;
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

    public List<Mesa> getMesas() {
        return mesas;
    }

    public void setMesas(List<Mesa> mesas) {
        this.mesas = mesas;
    }

    // Métodos auxiliares
    public boolean isSalon() {
        return "SALON".equals(this.tipo);
    }

    public boolean isBarra() {
        return "BARRA".equals(this.tipo);
    }

    public boolean cobraServicio() {
        return "S".equals(this.cobraServicio);
    }

    public boolean isActivo() {
        return "A".equals(this.estado);
    }

    public void addMesa(Mesa mesa) {
        mesas.add(mesa);
        mesa.setSalon(this);
    }

    public void removeMesa(Mesa mesa) {
        mesas.remove(mesa);
        mesa.setSalon(null);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Salon)) {
            return false;
        }
        Salon other = (Salon) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Salon[id=" + id + ", nombre=" + nombre + ", tipo=" + tipo + "]";
    }
}