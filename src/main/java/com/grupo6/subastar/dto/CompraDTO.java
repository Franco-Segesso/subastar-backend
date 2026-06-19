package com.grupo6.subastar.dto;

public record CompraDTO(
        Integer identificador,
        SubastaDTO subasta,
        ItemDTO item,
        Double importePujado,
        Double comision,
        Double costoEnvio,
        Double total,
        String modalidadEntrega,
        String direccionEnvio,
        String avisoSeguro,
        String estadoPago,
        Integer medioPagoId) {

    public record SubastaDTO(Integer identificador, String nombre, String moneda) {
    }

    public record ItemDTO(
            Integer identificador,
            Integer numeroPieza,
            String descripcionCatalogo) {
    }
}
