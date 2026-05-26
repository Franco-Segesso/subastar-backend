package com.grupo6.subastar.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "itemsCatalogo")
public class ItemCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "catalogo", referencedColumnName = "identificador")
    @JsonIgnore
    private Catalogo catalogo;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto", referencedColumnName = "identificador")
    private Producto producto;

    @Column(name = "precioBase", nullable = false)
    private Double precioBase;

    @Column(name = "comision", nullable = false)
    private Double comision;

    @Column(name = "subastado")
    private String subastado;

    public ItemCatalogo() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Catalogo getCatalogo() { return catalogo; }
    public void setCatalogo(Catalogo catalogo) { this.catalogo = catalogo; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Double getPrecioBase() { return precioBase; }
    public void setPrecioBase(Double precioBase) { this.precioBase = precioBase; }
    public Double getComision() { return comision; }
    public void setComision(Double comision) { this.comision = comision; }
    public String getSubastado() { return subastado; }
    public void setSubastado(String subastado) { this.subastado = subastado; }
}