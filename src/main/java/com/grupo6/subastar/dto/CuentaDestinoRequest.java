package com.grupo6.subastar.dto;

public class CuentaDestinoRequest {
    private String banco;
    private String cbu_iban;
    private String pais;
    private String moneda;

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getCbu_iban() { return cbu_iban; }
    public void setCbu_iban(String cbu_iban) { this.cbu_iban = cbu_iban; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
}
