package com.grupo6.subastar.dto;

public class EstadoPujaDTO {
    private Integer subastaId;
    private Integer itemId;
    private Integer tiempoRestanteSegundos;
    private Double importeActual;
    private boolean cerrado;

    public EstadoPujaDTO() {}

    public EstadoPujaDTO(Integer subastaId, Integer itemId, Integer tiempoRestanteSegundos, Double importeActual, boolean cerrado) {
        this.subastaId = subastaId;
        this.itemId = itemId;
        this.tiempoRestanteSegundos = tiempoRestanteSegundos;
        this.importeActual = importeActual;
        this.cerrado = cerrado;
    }

    public Integer getSubastaId() { return subastaId; }
    public void setSubastaId(Integer subastaId) { this.subastaId = subastaId; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public Integer getTiempoRestanteSegundos() { return tiempoRestanteSegundos; }
    public void setTiempoRestanteSegundos(Integer tiempoRestanteSegundos) { this.tiempoRestanteSegundos = tiempoRestanteSegundos; }
    public Double getImporteActual() { return importeActual; }
    public void setImporteActual(Double importeActual) { this.importeActual = importeActual; }
    public boolean isCerrado() { return cerrado; }
    public void setCerrado(boolean cerrado) { this.cerrado = cerrado; }
}
