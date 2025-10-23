/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "mesa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"salon_id", "identificador"})
})
@NamedQueries({
    @NamedQuery(name = "Mesa.findAll", query = "SELECT m FROM Mesa m"),
    @NamedQuery(name = "Mesa.findBySalon", query = "SELECT m FROM Mesa m WHERE m.salon.id = :salonId"),
    @NamedQuery(name = "Mesa.findLibres", query = "SELECT m FROM Mesa m WHERE m.estado = 'LIBRE'"),
    @NamedQuery(name = "Mesa.findOcupadas", query = "SELECT m FROM Mesa m WHERE m.estado = 'OCUPADA'")
})
public class Mesa implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mesa_seq")
    @SequenceGenerator(name = "mesa_seq", sequenceName = "seq_mesa", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @JsonIgnore 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "identificador", nullable = false, length = 20)
    private String identificador;

    @Column(name = "posicion_x")
    private Double posicionX = 0.0;

    @Column(name = "posicion_y")
    private Double posicionY = 0.0;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "LIBRE";

    @Version
    @Column(name = "version")
    private Long version;

    public Mesa() {
        this.estado = "LIBRE";
        this.posicionX = 0.0;
        this.posicionY = 0.0;
    }

    public Mesa(Long id) {
        this();
        this.id = id;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Salon getSalon() { return salon; }
    public void setSalon(Salon salon) { this.salon = salon; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public Double getPosicionX() { return posicionX; }
    public void setPosicionX(Double posicionX) { this.posicionX = posicionX; }
    public Double getPosicionY() { return posicionY; }
    public void setPosicionY(Double posicionY) { this.posicionY = posicionY; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // Métodos auxiliares
    public boolean isLibre() { return "LIBRE".equals(this.estado); }
    public boolean isOcupada() { return "OCUPADA".equals(this.estado); }
    
    public void ocupar() { this.estado = "OCUPADA"; }
    public void liberar() { this.estado = "LIBRE"; }

    @Transient
    public Long getSalonId() {
        return salon != null ? salon.getId() : null;
    }
    
    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Mesa)) return false;
        Mesa other = (Mesa) obj;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Mesa[id=" + id + ", identificador=" + identificador + ", estado=" + estado + "]";
    }
}