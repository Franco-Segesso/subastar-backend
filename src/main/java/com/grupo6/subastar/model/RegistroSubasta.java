package com.grupo6.subastar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registroDeSubasta")
public class RegistroSubasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "subasta", nullable = false)
    private Integer subastaId;

    @Column(name = "duenio", nullable = false)
    private Integer duenioId;

    @Column(name = "producto", nullable = false)
    private Integer productoId;

    @Column(name = "cliente", nullable = false)
    private Integer clienteId;

    @Column(name = "importe", nullable = false)
    private Double importe;

    @Column(name = "comision", nullable = false)
    private Double comision;

    @Column(name = "costoEnvio")
    private Double costoEnvio;

    @Column(name = "nroPolizaSeguro")
    private String nroPolizaSeguro;

    @Column(name = "modalidadEntrega", nullable = false)
    private String modalidadEntrega;

    public Integer getIdentificador() { return identificador; }
    public Integer getSubastaId() { return subastaId; }
    public void setSubastaId(Integer subastaId) { this.subastaId = subastaId; }
    public Integer getDuenioId() { return duenioId; }
    public void setDuenioId(Integer duenioId) { this.duenioId = duenioId; }
    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }
    public Double getComision() { return comision; }
    public void setComision(Double comision) { this.comision = comision; }
    public Double getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(Double costoEnvio) { this.costoEnvio = costoEnvio; }
    public String getNroPolizaSeguro() { return nroPolizaSeguro; }
    public void setNroPolizaSeguro(String nroPolizaSeguro) { this.nroPolizaSeguro = nroPolizaSeguro; }
    public String getModalidadEntrega() { return modalidadEntrega; }
    public void setModalidadEntrega(String modalidadEntrega) { this.modalidadEntrega = modalidadEntrega; }
}
