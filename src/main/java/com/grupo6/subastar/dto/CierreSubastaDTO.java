package com.grupo6.subastar.dto;

public class CierreSubastaDTO {
    private Integer itemId;
    private boolean hayGanador;
    private Integer idClienteGanador;
    private Double importeFinal;

    // Getters y Setters
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public boolean isHayGanador() { return hayGanador; }
    public void setHayGanador(boolean hayGanador) { this.hayGanador = hayGanador; }
    public Integer getIdClienteGanador() { return idClienteGanador; }
    public void setIdClienteGanador(Integer idClienteGanador) { this.idClienteGanador = idClienteGanador; }
    public Double getImporteFinal() { return importeFinal; }
    public void setImporteFinal(Double importeFinal) { this.importeFinal = importeFinal; }
}