package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    @Column(name = "descripcionCatalogo")
    private String tipo;

    @Column(name = "descripcionCompleta", nullable = false)
    private String descripcion;

    @Column(name = "duenio", nullable = false)
    private Integer duenio; // FK a personas/clientes

    @Transient
    @JsonProperty("nombreDuenioReal")
    private String nombreDuenioReal;

    @Column(name = "nombreArtista")
    private String artista;

    @Column(name = "fechaCreacion")
    private String fechaCreacion;

    @Column(name = "historia")
    private String historia;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Foto> fotos;

    public Producto() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getDuenio() { return duenio; }
    public void setDuenio(Integer duenio) { this.duenio = duenio; }
    public String getArtista() { return artista; }
    public void setArtista(String artista) { this.artista = artista; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getHistoria() { return historia; }
    public void setHistoria(String historia) { this.historia = historia; }
    public List<Foto> getFotos() { return fotos; }
    public void setFotos(List<Foto> fotos) { this.fotos = fotos; }
    
    @JsonProperty("nombreDuenioReal")
    public String getNombreDuenioReal() { return nombreDuenioReal; }
    public void setNombreDuenioReal(String nombreDuenioReal) { this.nombreDuenioReal = nombreDuenioReal; }
}