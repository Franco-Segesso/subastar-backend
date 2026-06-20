package com.grupo6.subastar.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class VigilianteVencimientosService {

    private static final ZoneId ZONA_NEGOCIO =
            ZoneId.of("America/Argentina/Buenos_Aires");

    private final MultaService multaService;

    public VigilanteVencimientosService(MultaService multaService) {
        this.multaService = multaService;
    }

    @Scheduled(fixedDelay = 60000)
    public void verificarVencimientosDePago() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_NEGOCIO);
        multaService.generarMultasVencidas(ahora);
        multaService.derivarVencidasALaJusticia(ahora);
    }
}
