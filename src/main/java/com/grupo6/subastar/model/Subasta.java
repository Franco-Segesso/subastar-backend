package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "subastas")
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "estado")
    private String estado;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "moneda", nullable = false)
    private String moneda;

    @Column(name = "ubicacion", nullable = false)
    private String ubicacion;

    @OneToOne(mappedBy = "subasta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Catalogo catalogo;

    @Transient
    private Double mejorOferta;

    @Transient
    private Integer cantidadPostores;

    @Transient
    private Integer itemActual;

    @Column(name = "capacidadAsistentes")
    private Integer capacidadAsistentes;

    public Subasta() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Catalogo getCatalogo() { return catalogo; }
    public void setCatalogo(Catalogo catalogo) { this.catalogo = catalogo; }
    public Double getMejorOferta() { return mejorOferta; }
    public void setMejorOferta(Double mejorOferta) { this.mejorOferta = mejorOferta; }
    public Integer getCantidadPostores() { return cantidadPostores; }
    public void setCantidadPostores(Integer cantidadPostores) { this.cantidadPostores = cantidadPostores; }
    public Integer getItemActual() { return itemActual; }
    public void setItemActual(Integer itemActual) { this.itemActual = itemActual; }
    public Integer getCapacidadAsistentes() { return capacidadAsistentes; }
    public void setCapacidadAsistentes(Integer capacidadAsistentes) { this.capacidadAsistentes = capacidadAsistentes; }
}