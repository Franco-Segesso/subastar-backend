package com.grupo6.subastar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "seguros")
public class Seguro {

    @Id
    @Column(name = "nroPoliza", length = 30)
    private String nroPoliza;

    @Column(name = "compania", nullable = false)
    private String compania;

    @Column(name = "polizaCombinada")
    private String polizaCombinada;

    @Column(name = "importe", nullable = false)
    private BigDecimal importe;

    public String getNroPoliza() { return nroPoliza; }
    public void setNroPoliza(String nroPoliza) { this.nroPoliza = nroPoliza; }
    public String getCompania() { return compania; }
    public void setCompania(String compania) { this.compania = compania; }
    public String getPolizaCombinada() { return polizaCombinada; }
    public void setPolizaCombinada(String polizaCombinada) { this.polizaCombinada = polizaCombinada; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
}
