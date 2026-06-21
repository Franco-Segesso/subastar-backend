package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cuentasDestino")
public class CuentaDestino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duenio", referencedColumnName = "identificador", nullable = false)
    private Duenio duenio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud", referencedColumnName = "identificador")
    private SolicitudConsignacion solicitud;

    @Column(name = "banco", nullable = false)
    private String banco;

    @Column(name = "CBU_IBAN", nullable = false)
    private String cbuIban;

    @Column(name = "pais", nullable = false)
    private String pais;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "activa", nullable = false)
    private String activa;

    public Integer getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Integer identificador) {
        this.identificador = identificador;
    }

    public Duenio getDuenio() {
        return duenio;
    }

    public void setDuenio(Duenio duenio) {
        this.duenio = duenio;
    }

    public SolicitudConsignacion getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudConsignacion solicitud) {
        this.solicitud = solicitud;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getCbuIban() {
        return cbuIban;
    }

    public void setCbuIban(String cbuIban) {
        this.cbuIban = cbuIban;
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

    public String getActiva() {
        return activa;
    }

    public void setActiva(String activa) {
        this.activa = activa;
    }
}