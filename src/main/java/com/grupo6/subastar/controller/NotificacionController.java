package com.grupo6.subastar.controller;

import com.grupo6.subastar.model.Notificacion;
import com.grupo6.subastar.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clientes/me/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    // GET /clientes/me/notificaciones
    @GetMapping
    public ResponseEntity<List<Notificacion>> getMisNotificaciones(
            Authentication authentication,
            @RequestParam(required = false) Boolean leidas) { // <- Filtro agregado
        String email = authentication.getName();
        List<Notificacion> notificaciones = notificacionService.obtenerMisNotificaciones(email, leidas);
        return ResponseEntity.ok(notificaciones);
    }

    // PATCH /clientes/me/notificaciones/leer-todas
    @PatchMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(Authentication authentication) {
        String email = authentication.getName();
        int cantidad = notificacionService.marcarTodasComoLeidas(email);
        
        // Cumplimos con el contrato exacto
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Todas las notificaciones fueron marcadas como leídas.");
        response.put("cantidadActualizada", cantidad);
        
        return ResponseEntity.ok(response);
    }

    // PATCH /clientes/me/notificaciones/{id}/leer
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Map<String, String>> marcarComoLeida(@PathVariable Integer id, Authentication authentication) {
        String email = authentication.getName();
        notificacionService.marcarComoLeida(id, email);
        
        // Cumplimos con el contrato exacto
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Notificación marcada como leída");
        
        return ResponseEntity.ok(response);
    }
}