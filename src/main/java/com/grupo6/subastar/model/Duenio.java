package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "duenios")
public class Duenio {

    @Id
    // ATENCIÓN: No lleva @GeneratedValue porque el ID ya viene de la persona
    @Column(name = "identificador")
    private Integer id;

    // Relación 1 a 1 con la Persona (Comparten el mismo ID)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", referencedColumnName = "identificador", insertable = false, updatable = false)
    private Persona persona;

    public Duenio() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }
}