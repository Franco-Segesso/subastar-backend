package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tarjetasCredito")
public class TarjetaCredito extends MedioPago {

    @Column(name = "ultimosDigitos", length = 4, nullable = false)
    private String ultimosDigitos;

    @Column(name = "vencimiento", length = 5, nullable = false)
    private String vencimiento;

    @Column(name = "titular", length = 150, nullable = false)
    private String titular;

    @Column(name = "esExtranjera", length = 2)
    private String esExtranjera;

    @Column(name = "paisEmisor", length = 150)
    private String paisEmisor;

    public TarjetaCredito() { setTipo("tarjeta"); }

    public String getUltimosDigitos() { return ultimosDigitos; }
    public void setUltimosDigitos(String ultimosDigitos) { this.ultimosDigitos = ultimosDigitos; }
    public String getVencimiento() { return vencimiento; }
    public void setVencimiento(String vencimiento) { this.vencimiento = vencimiento; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public String getEsExtranjera() { return esExtranjera; }
    public void setEsExtranjera(String esExtranjera) { this.esExtranjera = esExtranjera; }
    public String getPaisEmisor() { return paisEmisor; }
    public void setPaisEmisor(String paisEmisor) { this.paisEmisor = paisEmisor; }
}