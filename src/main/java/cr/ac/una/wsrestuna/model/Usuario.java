/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Entidad que representa los usuarios del sistema
 * Roles: ADMINISTRATIVO (acceso total), CAJERO (facturación), SALONERO (solo órdenes)
 * 
 * @author Tu Nombre
 */
@Entity
@Table(name = "usuario")
@NamedQueries({
    @NamedQuery(name = "Usuario.findAll", query = "SELECT u FROM Usuario u"),
    @NamedQuery(name = "Usuario.findByUsuario", query = "SELECT u FROM Usuario u WHERE u.usuario = :usuario"),
    @NamedQuery(name = "Usuario.findByRol", query = "SELECT u FROM Usuario u WHERE u.rol = :rol"),
    @NamedQuery(name = "Usuario.findActivos", query = "SELECT u FROM Usuario u WHERE u.estado = 'A'")
})
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "seq_usuario", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String usuario;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "clave", nullable = false, length = 255)
    private String clave;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "rol", nullable = false, length = 20)
    private String rol; // ADMINISTRATIVO, CAJERO, SALONERO

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A"; // A=Activo, I=Inactivo

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Version
    @Column(name = "version")
    private Long version;

    // Constructores
    public Usuario() {
        this.estado = "A";
        this.fechaCreacion = LocalDate.now();
    }

    public Usuario(Long id) {
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Métodos auxiliares
    public boolean isActivo() {
        return "A".equals(this.estado);
    }

    public boolean isAdministrativo() {
        return "ADMINISTRATIVO".equals(this.rol);
    }

    public boolean isCajero() {
        return "CAJERO".equals(this.rol);
    }

    public boolean isSalonero() {
        return "SALONERO".equals(this.rol);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Usuario)) {
            return false;
        }
        Usuario other = (Usuario) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", usuario=" + usuario + ", rol=" + rol + "]";
    }
}