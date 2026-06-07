package com.grupo6.subastar.dto;

public class EstadoSubastaDTO {
    private Integer subastaId;
    private String estado;

    public EstadoSubastaDTO(Integer subastaId, String estado) {
        this.subastaId = subastaId;
        this.estado = estado;
    }

    public Integer getSubastaId() {
        return subastaId;
    }

    public String getEstado() {
        return estado;
    }
}
