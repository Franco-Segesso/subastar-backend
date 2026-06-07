package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "mediosDePago")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo;



    @Column(name = "activo", length = 2)
    private String activo = "si";

    @Column(name = "fechaAlta")
    private LocalDate fechaAlta = LocalDate.now();

    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getActivo() { return activo; }
    public void setActivo(String activo) { this.activo = activo; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
}