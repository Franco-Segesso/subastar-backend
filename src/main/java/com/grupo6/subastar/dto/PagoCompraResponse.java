package com.grupo6.subastar.dto;

public record PagoCompraResponse(
        String mensaje,
        Integer compraId,
        String estadoPago,
        Integer medioPagoId) {
}
