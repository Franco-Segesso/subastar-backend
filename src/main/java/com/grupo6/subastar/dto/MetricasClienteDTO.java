package com.grupo6.subastar.dto;

import java.util.List;

public record MetricasClienteDTO(
        Integer totalSubastas,
        Integer subastaGanadas,
        Double porcentajeExito,
        Double totalPujado,
        Double totalPagado,
        Integer totalPujas,
        Double promedioPujasPorSubasta,
        Double pujaMaxima,
        List<ActividadCategoriaDTO> actividadPorCategoria,
        String categoriaActual,
        Integer consignaciones) {

    public record ActividadCategoriaDTO(String categoria, Integer cantidadPujas) {
    }
}
