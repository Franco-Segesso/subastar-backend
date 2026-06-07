package com.grupo6.subastar.dto;

public class PujaMensajeDTO {
    private Double importe;
    private String fechaHora;
    private AsistenteDTO asistente;

    public PujaMensajeDTO(Double importe, String fechaHora, Integer clienteId) {
        this.importe = importe;
        this.fechaHora = fechaHora;
        this.asistente = new AsistenteDTO(clienteId);
    }

    public Double getImporte() { return importe; }
    public String getFechaHora() { return fechaHora; }
    public AsistenteDTO getAsistente() { return asistente; }

    public static class AsistenteDTO {
        private ClienteDTO cliente;
        public AsistenteDTO(Integer clienteId) { this.cliente = new ClienteDTO(clienteId); }
        public ClienteDTO getCliente() { return cliente; }
    }

    public static class ClienteDTO {
        private Integer identificador;
        public ClienteDTO(Integer identificador) { this.identificador = identificador; }
        public Integer getIdentificador() { return identificador; }
    }
}