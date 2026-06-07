package com.grupo6.subastar.dto;

public class PujaMensajeDTO {
    private Integer itemId;
    private Double importe;
    private String fechaHora;
    private AsistenteDTO asistente;

    public PujaMensajeDTO(Double importe, String fechaHora, Integer clienteId) {
        this.importe = importe;
        this.fechaHora = fechaHora;
        this.asistente = new AsistenteDTO(clienteId);
    }

    public PujaMensajeDTO(Integer itemId, Double importe, String fechaHora, Integer clienteId, String nombre, String apellido) {
        this.itemId = itemId;
        this.importe = importe;
        this.fechaHora = fechaHora;
        this.asistente = new AsistenteDTO(clienteId, nombre, apellido);
    }

    public Integer getItemId() { return itemId; }
    public Double getImporte() { return importe; }
    public String getFechaHora() { return fechaHora; }
    public AsistenteDTO getAsistente() { return asistente; }

    public static class AsistenteDTO {
        private ClienteDTO cliente;
        public AsistenteDTO(Integer clienteId) { this.cliente = new ClienteDTO(clienteId); }
        public AsistenteDTO(Integer clienteId, String nombre, String apellido) { this.cliente = new ClienteDTO(clienteId, nombre, apellido); }
        public ClienteDTO getCliente() { return cliente; }
    }

    public static class ClienteDTO {
        private Integer identificador;
        private String nombre;
        private String apellido;
        public ClienteDTO(Integer identificador) { this.identificador = identificador; }
        public ClienteDTO(Integer identificador, String nombre, String apellido) {
            this.identificador = identificador;
            this.nombre = nombre;
            this.apellido = apellido;
        }
        public Integer getIdentificador() { return identificador; }
        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
    }
}
