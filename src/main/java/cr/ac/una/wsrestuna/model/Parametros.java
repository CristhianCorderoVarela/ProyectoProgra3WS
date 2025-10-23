/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entidad que representa los parámetros generales del sistema
 * Configuración de idioma, impuestos, descuentos y datos del restaurante
 * 
 * @author Tu Nombre
 */
@Entity
@Table(name = "parametros")
@NamedQueries({
    @NamedQuery(name = "Parametros.findAll", query = "SELECT p FROM Parametros p")
})
public class Parametros implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "parametros_seq")
    @SequenceGenerator(name = "parametros_seq", sequenceName = "seq_parametros", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 10)
    @Column(name = "idioma", nullable = false, length = 10)
    private String idioma = "es"; // es, en

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "porc_impuesto_venta", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcImpuestoVenta = new BigDecimal("13.00");

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "porc_impuesto_servicio", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcImpuestoServicio = new BigDecimal("10.00");

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "porc_descuento_maximo", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcDescuentoMaximo = new BigDecimal("10.00");

    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "nombre_restaurante", nullable = false, length = 150)
    private String nombreRestaurante;

    @Size(max = 20)
    @Column(name = "telefono1", length = 20)
    private String telefono1;

    @Size(max = 20)
    @Column(name = "telefono2", length = 20)
    private String telefono2;

    @Size(max = 250)
    @Column(name = "direccion", length = 250)
    private String direccion;

    @Size(max = 100)
    @Column(name = "correo_sistema", length = 100)
    private String correoSistema;

    @Size(max = 255)
    @Column(name = "clave_correo_sistema", length = 255)
    private String claveCorreoSistema;

    @Version
    @Column(name = "version")
    private Long version;

    // Constructores
    public Parametros() {
        this.idioma = "es";
        this.porcImpuestoVenta = new BigDecimal("13.00");
        this.porcImpuestoServicio = new BigDecimal("10.00");
        this.porcDescuentoMaximo = new BigDecimal("10.00");
    }

    public Parametros(Long id) {
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

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public BigDecimal getPorcImpuestoVenta() {
        return porcImpuestoVenta;
    }

    public void setPorcImpuestoVenta(BigDecimal porcImpuestoVenta) {
        this.porcImpuestoVenta = porcImpuestoVenta;
    }

    public BigDecimal getPorcImpuestoServicio() {
        return porcImpuestoServicio;
    }

    public void setPorcImpuestoServicio(BigDecimal porcImpuestoServicio) {
        this.porcImpuestoServicio = porcImpuestoServicio;
    }

    public BigDecimal getPorcDescuentoMaximo() {
        return porcDescuentoMaximo;
    }

    public void setPorcDescuentoMaximo(BigDecimal porcDescuentoMaximo) {
        this.porcDescuentoMaximo = porcDescuentoMaximo;
    }

    public String getNombreRestaurante() {
        return nombreRestaurante;
    }

    public void setNombreRestaurante(String nombreRestaurante) {
        this.nombreRestaurante = nombreRestaurante;
    }

    public String getTelefono1() {
        return telefono1;
    }

    public void setTelefono1(String telefono1) {
        this.telefono1 = telefono1;
    }

    public String getTelefono2() {
        return telefono2;
    }

    public void setTelefono2(String telefono2) {
        this.telefono2 = telefono2;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCorreoSistema() {
        return correoSistema;
    }

    public void setCorreoSistema(String correoSistema) {
        this.correoSistema = correoSistema;
    }

    public String getClaveCorreoSistema() {
        return claveCorreoSistema;
    }

    public void setClaveCorreoSistema(String claveCorreoSistema) {
        this.claveCorreoSistema = claveCorreoSistema;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Métodos auxiliares
    public BigDecimal calcularImpuestoVenta(BigDecimal monto) {
        if (monto == null || porcImpuestoVenta == null) {
            return BigDecimal.ZERO;
        }
        return monto.multiply(porcImpuestoVenta).divide(new BigDecimal("100"));
    }

    public BigDecimal calcularImpuestoServicio(BigDecimal monto) {
        if (monto == null || porcImpuestoServicio == null) {
            return BigDecimal.ZERO;
        }
        return monto.multiply(porcImpuestoServicio).divide(new BigDecimal("100"));
    }

    public boolean validarDescuento(BigDecimal porcentajeDescuento) {
        if (porcentajeDescuento == null || porcDescuentoMaximo == null) {
            return false;
        }
        return porcentajeDescuento.compareTo(porcDescuentoMaximo) <= 0;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Parametros)) {
            return false;
        }
        Parametros other = (Parametros) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "Parametros[id=" + id + ", restaurante=" + nombreRestaurante + "]";
    }
}