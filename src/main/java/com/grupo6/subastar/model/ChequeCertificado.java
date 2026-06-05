package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "chequesCertificados")
public class ChequeCertificado extends MedioPago {

    @Column(name = "nroCheque", length = 50, nullable = false)
    private String nroCheque;

    @Column(name = "banco", length = 150, nullable = false)
    private String banco;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @Column(name = "montoGarantia", precision = 18, scale = 2, nullable = false)
    private BigDecimal montoGarantia;

    @Column(name = "verificado", length = 2)
    private String verificadoCheque = "no";

    @Column(name = "fechaEntrega", nullable = false)
    private LocalDate fechaEntrega;

    public ChequeCertificado() { setTipo("cheque"); }

    public String getNroCheque() { return nroCheque; }
    public void setNroCheque(String nroCheque) { this.nroCheque = nroCheque; }
    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public BigDecimal getMontoGarantia() { return montoGarantia; }
    public void setMontoGarantia(BigDecimal montoGarantia) { this.montoGarantia = montoGarantia; }
    public String getVerificadoCheque() { return verificadoCheque; }
    public void setVerificadoCheque(String verificadoCheque) { this.verificadoCheque = verificadoCheque; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}