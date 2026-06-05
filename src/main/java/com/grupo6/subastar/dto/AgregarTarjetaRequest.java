package com.grupo6.subastar.dto;

public class AgregarTarjetaRequest {
    private String ultimosDigitos;
    private String vencimiento;
    private String titular;
    private String esExtranjera;
    private String paisEmisor;

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