/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa grupos o categorías de productos
 * Ejemplos: bebidas calientes, bebidas frías, platos fuertes, entradas
 * 
 * @author Tu Nombre
 */
@Entity
@Table(name = "grupo_producto")
@NamedQueries({
    @NamedQuery(name = "GrupoProducto.findAll", query = "SELECT g FROM GrupoProducto g"),
    @NamedQuery(name = "GrupoProducto.findActivos", query = "SELECT g FROM GrupoProducto g WHERE g.estado = 'A'"),
    @NamedQuery(name = "GrupoProducto.findMenuRapido", 
                query = "SELECT g FROM GrupoProducto g WHERE g.menuRapido = 'S' AND g.estado = 'A' ORDER BY g.totalVentas DESC")
})
public class GrupoProducto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grupo_producto_seq")
    @SequenceGenerator(name = "grupo_producto_seq", sequenceName = "seq_grupo_producto", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "menu_rapido", nullable = false, length = 1)
    private String menuRapido = "N"; // S=Sí, N=No

    @Column(name = "total_ventas")
    private Long totalVentas = 0L;

    @NotNull
    @Size(min = 1, max = 1)
    @Column(name = "estado", nullable = false, length = 1)
    private String estado = "A"; // A=Activo, I=Inactivo

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL)
    @JsonbTransient
    private List<Producto> productos;

    // Constructores
    public GrupoProducto() {
        this.estado = "A";
        this.menuRapido = "N";
        this.totalVentas = 0L;
        this.productos = new ArrayList<>();
    }

    public GrupoProducto(Long id) {
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

    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    // Métodos auxiliares
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
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof GrupoProducto)) {
            return false;
        }
        GrupoProducto other = (GrupoProducto) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "GrupoProducto[id=" + id + ", nombre=" + nombre + "]";
    }
}