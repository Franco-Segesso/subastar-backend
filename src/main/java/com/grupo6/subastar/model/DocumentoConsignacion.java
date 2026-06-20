package com.grupo6.subastar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentosConsignacion")
public class DocumentoConsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud", nullable = false)
    private SolicitudConsignacion solicitud;

    @Column(name = "urlArchivo", nullable = false)
    private String urlArchivo;

    @Column(name = "nombreArchivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fechaCarga")
    private LocalDateTime fechaCarga;

    @Column(name = "estado")
    private String estado;

    public Integer getIdentificador() { return identificador; }
    public SolicitudConsignacion getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudConsignacion solicitud) { this.solicitud = solicitud; }
    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
