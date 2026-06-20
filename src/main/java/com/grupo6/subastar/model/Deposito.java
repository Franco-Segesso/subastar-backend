package com.grupo6.subastar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "depositos")
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "sector")
    private String sector;

    @Column(name = "capacidad")
    private Integer capacidad;

    public Integer getIdentificador() { return identificador; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getSector() { return sector; }
    public Integer getCapacidad() { return capacidad; }
}
