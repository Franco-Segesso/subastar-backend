package com.grupo6.subastar.dto;

import java.math.BigDecimal;

public class AgregarCuentaRequest {
    private String cbuIban;
    private String alias;
    private String banco;
    private String paisBanco;
    private BigDecimal fondosReservados;
    private String moneda;

    public String getCbuIban() { return cbuIban; }
    public void setCbuIban(String cbuIban) { this.cbuIban = cbuIban; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getPaisBanco() { return paisBanco; }
    public void setPaisBanco(String paisBanco) { this.paisBanco = paisBanco; }
    public BigDecimal getFondosReservados() { return fondosReservados; }
    public void setFondosReservados(BigDecimal fondosReservados) { this.fondosReservados = fondosReservados; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
}