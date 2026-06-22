package com.grupo6.subastar.dto;

public class PujaRequest {
    private Integer itemId;
    private Double importe;
    private Integer medioPagoId;
    private String modalidadEntrega;

    // Getters y Setters
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }
    public Integer getMedioPagoId() { return medioPagoId; }
    public void setMedioPagoId(Integer medioPagoId) { this.medioPagoId = medioPagoId; }
    public String getModalidadEntrega() { return modalidadEntrega; }
    public void setModalidadEntrega(String modalidadEntrega) { this.modalidadEntrega = modalidadEntrega; }
}
