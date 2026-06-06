package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "asistentes") // Poné el nombre real de la tabla acá
public class Asistente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    // Relación directa con el Cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente", referencedColumnName = "identificador")
    private Cliente cliente;

    public Asistente() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}