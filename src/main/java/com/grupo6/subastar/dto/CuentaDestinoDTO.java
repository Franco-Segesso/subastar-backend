package com.grupo6.subastar.dto;

public class CuentaDestinoDTO {

    private Integer identificador;
    private String banco;
    private String cbu_iban;
    private String pais;
    private String moneda;

    public CuentaDestinoDTO() {
    }

    public CuentaDestinoDTO(Integer identificador, String banco, String cbu_iban, String pais, String moneda) {
        this.identificador = identificador;
        this.banco = banco;
        this.cbu_iban = cbu_iban;
        this.pais = pais;
        this.moneda = moneda;
    }

    public Integer getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Integer identificador) {
        this.identificador = identificador;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getCbu_iban() {
        return cbu_iban;
    }

    public void setCbu_iban(String cbu_iban) {
        this.cbu_iban = cbu_iban;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }
}