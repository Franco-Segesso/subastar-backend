package com.grupo6.subastar.service;

import com.grupo6.subastar.model.TipoNotificacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class VigilanteVencimientosService {

    @Autowired
    private NotificacionService notificacionService;

    // Cron: Se ejecuta automáticamente en el minuto 0 de cada hora
    @Scheduled(cron = "0 0 * * * *")
    public void verificarVencimientosDePago() {
        System.out.println(">> Iniciando revisión automática de pagos pendientes y multas...");

        // NOTA PARA EL EQUIPO: 
        // Cuando en el Sprint 2 se implemente el flujo de "Pagos" y "Multas",
        // aquí se deben hacer las consultas a la base de datos para buscar:
        // 1. Ítems ganados hace 42hs sin pagar (Para lanzar aviso previo)
        // 2. Ítems ganados hace 48hs sin pagar (Para crear la multa en BD y bloquear usuario)

        /* EJEMPLO DE CÓMO SE DISPARARÁ LA NOTIFICACIÓN DE MULTA:
        
        for (Multa nuevaMulta : multasGeneradasHoy) {
            notificacionService.crearNotificacion(
                nuevaMulta.getCliente(),
                "Multa Aplicada",
                "El tiempo para pagar tu ítem ha expirado. Se ha generado una multa de USD " + nuevaMulta.getImporte(),
                TipoNotificacion.MULTA,
                nuevaMulta.getIdentificador() // ID de la multa para la redirección en Android
            );
        }
        */
    }
}