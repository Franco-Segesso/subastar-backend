package com.grupo6.subastar.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SubastaParticipacionDTO(
        SubastaDTO subasta,
        Integer cantidadPujas,
        Double mayorOfertaRealizada,
        Boolean gano,
        Integer loteGanado,
        Double importePagado,
        Integer compraId,
        String estadoPago) {

    public record SubastaDTO(
            Integer identificador,
            String nombre,
            LocalDate fecha,
            LocalTime hora,
            String categoria,
            String moneda) {
    }
}
