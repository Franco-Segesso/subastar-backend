package com.grupo6.subastar.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MultaDTO(
        Integer identificador,
        BigDecimal importe,
        String estado,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaVencimiento,
        LocalDateTime fechaPago,
        String subasta,
        Double importeOfertado,
        Integer compraId,
        String estadoCompra,
        Double totalCompra) {
}
