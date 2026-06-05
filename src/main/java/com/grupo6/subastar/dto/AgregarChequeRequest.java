package com.grupo6.subastar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AgregarChequeRequest {
    private String nroCheque;
    private String banco;
    private String moneda;
    private BigDecimal montoGarantia;
    private LocalDate fechaEntrega;

    public String getNroCheque() { return nroCheque; }
    public void setNroCheque(String nroCheque) { this.nroCheque = nroCheque; }
    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public BigDecimal getMontoGarantia() { return montoGarantia; }
    public void setMontoGarantia(BigDecimal montoGarantia) { this.montoGarantia = montoGarantia; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}