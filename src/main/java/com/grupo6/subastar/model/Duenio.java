package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "duenios")
public class Duenio {

    @Id
    // No lleva @GeneratedValue porque el ID ya viene de la persona
    @Column(name = "identificador")
    private Integer id;

    // Relación 1 a 1 con la Persona (Comparten el mismo ID)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", referencedColumnName = "identificador", insertable = false, updatable = false)
    private Persona persona;

    @Column(name = "numeroPais")
    private Integer numeroPais;

    @Column(name = "calificacionRiesgo")
    private Integer calificacionRiesgo;

    @Column(name = "verificador", nullable = false)
    private Integer verificadorId;

    public Duenio() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Integer getNumeroPais() { return numeroPais; }
    public void setNumeroPais(Integer numeroPais) { this.numeroPais = numeroPais; }

    public Integer getCalificacionRiesgo() { return calificacionRiesgo; }
    public void setCalificacionRiesgo(Integer calificacionRiesgo) { this.calificacionRiesgo = calificacionRiesgo; }

    public Integer getVerificadorId() { return verificadorId; }
    public void setVerificadorId(Integer verificadorId) { this.verificadorId = verificadorId; }
}
