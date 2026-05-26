package com.grupo6.subastar.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "catalogos")
public class Catalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @OneToOne
    @JoinColumn(name = "subasta", referencedColumnName = "identificador")
    @JsonIgnore // Evita bucles infinitos al serializar a JSON
    private Subasta subasta;

    @OneToMany(mappedBy = "catalogo", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ItemCatalogo> items;

    public Catalogo() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDescription() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Subasta getSubasta() { return subasta; }
    public void setSubasta(Subasta subasta) { this.subasta = subasta; }
    public List<ItemCatalogo> getItems() { return items; }
    public void setItems(List<ItemCatalogo> items) { this.items = items; }
}