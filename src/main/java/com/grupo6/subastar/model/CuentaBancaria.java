package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cuentasBancarias")
public class CuentaBancaria extends MedioPago {

    @Column(name = "CBU_IBAN", length = 50, nullable = false)
    private String cbuIban;

    @Column(name = "alias", length = 150)
    private String alias;

    @Column(name = "banco", length = 150, nullable = false)
    private String banco;

    @Column(name = "paisBanco", length = 150)
    private String paisBanco;

    @Column(name = "fondosReservados", precision = 18, scale = 2)
    private BigDecimal fondosReservados = BigDecimal.ZERO;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda;

    @Transient
    private BigDecimal fondosDisponibles;

    public CuentaBancaria() { setTipo("cuenta"); }

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
    public BigDecimal getFondosDisponibles() { return fondosDisponibles; }
    public void setFondosDisponibles(BigDecimal fondosDisponibles) { this.fondosDisponibles = fondosDisponibles; }
}
