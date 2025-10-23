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

@Entity
@Table(name = "cierre_caja")
@NamedQueries({
    @NamedQuery(name = "CierreCaja.findAll", query = "SELECT c FROM CierreCaja c"),
    @NamedQuery(name = "CierreCaja.findByUsuario", query = "SELECT c FROM CierreCaja c WHERE c.usuario.id = :usuarioId ORDER BY c.fechaApertura DESC"),
    @NamedQuery(name = "CierreCaja.findAbiertos", query = "SELECT c FROM CierreCaja c WHERE c.estado = 'ABIERTO'"),
    @NamedQuery(name = "CierreCaja.findAbiertoByUsuario", 
                query = "SELECT c FROM CierreCaja c WHERE c.usuario.id = :usuarioId AND c.estado = 'ABIERTO'"),
    @NamedQuery(name = "CierreCaja.findByFecha", 
                query = "SELECT c FROM CierreCaja c WHERE c.fechaApertura BETWEEN :fechaInicio AND :fechaFin ORDER BY c.fechaApertura DESC")
})
public class CierreCaja implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cierre_caja_seq")
    @SequenceGenerator(name = "cierre_caja_seq", sequenceName = "seq_cierre_caja", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @DecimalMin(value = "0.0")
    @Column(name = "efectivo_declarado", precision = 10, scale = 2)
    private BigDecimal efectivoDeclarado;

    @DecimalMin(value = "0.0")
    @Column(name = "tarjeta_declarado", precision = 10, scale = 2)
    private BigDecimal tarjetaDeclarado;

    @Column(name = "efectivo_sistema", precision = 10, scale = 2)
    private BigDecimal efectivoSistema = BigDecimal.ZERO;

    @Column(name = "tarjeta_sistema", precision = 10, scale = 2)
    private BigDecimal tarjetaSistema = BigDecimal.ZERO;

    @Column(name = "diferencia_efectivo", precision = 10, scale = 2)
    private BigDecimal diferenciaEfectivo = BigDecimal.ZERO;

    @Column(name = "diferencia_tarjeta", precision = 10, scale = 2)
    private BigDecimal diferenciaTarjeta = BigDecimal.ZERO;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ABIERTO"; // ABIERTO, CERRADO

    @Version
    @Column(name = "version")
    private Long version;

    public CierreCaja() {
        this.estado = "ABIERTO";
        this.fechaApertura = LocalDateTime.now();
        this.efectivoSistema = BigDecimal.ZERO;
        this.tarjetaSistema = BigDecimal.ZERO;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public BigDecimal getEfectivoDeclarado() { return efectivoDeclarado; }
    public void setEfectivoDeclarado(BigDecimal efectivoDeclarado) { this.efectivoDeclarado = efectivoDeclarado; }
    public BigDecimal getTarjetaDeclarado() { return tarjetaDeclarado; }
    public void setTarjetaDeclarado(BigDecimal tarjetaDeclarado) { this.tarjetaDeclarado = tarjetaDeclarado; }
    public BigDecimal getEfectivoSistema() { return efectivoSistema; }
    public void setEfectivoSistema(BigDecimal efectivoSistema) { this.efectivoSistema = efectivoSistema; }
    public BigDecimal getTarjetaSistema() { return tarjetaSistema; }
    public void setTarjetaSistema(BigDecimal tarjetaSistema) { this.tarjetaSistema = tarjetaSistema; }
    public BigDecimal getDiferenciaEfectivo() { return diferenciaEfectivo; }
    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) { this.diferenciaEfectivo = diferenciaEfectivo; }
    public BigDecimal getDiferenciaTarjeta() { return diferenciaTarjeta; }
    public void setDiferenciaTarjeta(BigDecimal diferenciaTarjeta) { this.diferenciaTarjeta = diferenciaTarjeta; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public boolean isAbierto() { return "ABIERTO".equals(this.estado); }
    public boolean isCerrado() { return "CERRADO".equals(this.estado); }

    public void calcularDiferencias() {
        if (efectivoDeclarado != null && efectivoSistema != null) {
            this.diferenciaEfectivo = efectivoDeclarado.subtract(efectivoSistema);
        }
        if (tarjetaDeclarado != null && tarjetaSistema != null) {
            this.diferenciaTarjeta = tarjetaDeclarado.subtract(tarjetaSistema);
        }
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CierreCaja)) return false;
        CierreCaja other = (CierreCaja) obj;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }
}