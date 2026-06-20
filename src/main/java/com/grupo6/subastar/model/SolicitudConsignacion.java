package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudesConsignacion")
public class SolicitudConsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto", referencedColumnName = "identificador", nullable = false)
    private Producto producto;

    @Column(name = "estado")
    private String estado;

    @Column(name = "motivoRechazo")
    private String motivoRechazo;

    @Column(name = "fechaSolicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "condicionesAceptadas")
    private String condicionesAceptadas;

    @Column(name = "motivoDocumentacion")
    private String motivoDocumentacion;

    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public String getCondicionesAceptadas() { return condicionesAceptadas; }
    public void setCondicionesAceptadas(String condicionesAceptadas) { this.condicionesAceptadas = condicionesAceptadas; }
    public String getMotivoDocumentacion() { return motivoDocumentacion; }
    public void setMotivoDocumentacion(String motivoDocumentacion) { this.motivoDocumentacion = motivoDocumentacion; }
}
