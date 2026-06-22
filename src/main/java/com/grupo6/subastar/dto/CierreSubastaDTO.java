package com.grupo6.subastar.dto;

public class CierreSubastaDTO {
    private Integer itemId;
    private boolean hayGanador;
    private Integer idClienteGanador;
    private Double importeFinal;
    private Integer compraId;
    private boolean pagoAutomatico;
    private boolean multaGenerada;
    private String mensajePago;

    // Getters y Setters
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public boolean isHayGanador() { return hayGanador; }
    public void setHayGanador(boolean hayGanador) { this.hayGanador = hayGanador; }
    public Integer getIdClienteGanador() { return idClienteGanador; }
    public void setIdClienteGanador(Integer idClienteGanador) { this.idClienteGanador = idClienteGanador; }
    public Double getImporteFinal() { return importeFinal; }
    public void setImporteFinal(Double importeFinal) { this.importeFinal = importeFinal; }
    public Integer getCompraId() { return compraId; }
    public void setCompraId(Integer compraId) { this.compraId = compraId; }
    public boolean isPagoAutomatico() { return pagoAutomatico; }
    public void setPagoAutomatico(boolean pagoAutomatico) { this.pagoAutomatico = pagoAutomatico; }
    public boolean isMultaGenerada() { return multaGenerada; }
    public void setMultaGenerada(boolean multaGenerada) { this.multaGenerada = multaGenerada; }
    public String getMensajePago() { return mensajePago; }
    public void setMensajePago(String mensajePago) { this.mensajePago = mensajePago; }
}
