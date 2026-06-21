package com.grupo6.subastar.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HistorialPujasClienteDTO(
        SubastaDTO subasta,
        ResumenDTO resumen,
        List<PujaDTO> pujas) {

    public record SubastaDTO(Integer identificador, String nombre, String moneda) {
    }

    public record ResumenDTO(
            Double totalPujado,
            Double totalPagado,
            Integer cantidadPujas,
            Boolean gano) {
    }

    public record PujaDTO(
            Integer orden,
            Integer itemId,
            String descripcionItem,
            Double importe,
            LocalDateTime fechaHora,
            Boolean esGanadora,
            Integer compraId,
            String estadoPago,
            SuperadaPorDTO superadaPor) {
    }

    public record SuperadaPorDTO(String postor, Double importe) {
    }
}
