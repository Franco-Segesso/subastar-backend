package com.grupo6.subastar.controller;

import com.grupo6.subastar.service.ClientePujaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes/me")
public class ClientePujaController {

    private final ClientePujaService clientePujaService;

    public ClientePujaController(ClientePujaService clientePujaService) {
        this.clientePujaService = clientePujaService;
    }

    @GetMapping("/metricas")
    public ResponseEntity<?> obtenerMetricas() {
        try {
            return ResponseEntity.ok(clientePujaService.obtenerMetricas(emailAutenticado()));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    @GetMapping("/subastas")
    public ResponseEntity<?> listarSubastas(
            @RequestParam(required = false) String resultado) {
        try {
            return ResponseEntity.ok(
                    clientePujaService.listarSubastas(emailAutenticado(), resultado));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    @GetMapping("/subastas/{subastaId}/pujas")
    public ResponseEntity<?> obtenerHistorial(
            @PathVariable Integer subastaId) {
        try {
            return ResponseEntity.ok(
                    clientePujaService.obtenerHistorial(emailAutenticado(), subastaId));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ResponseEntity<String> manejarExcepcion(RuntimeException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        if (mensaje.startsWith("401")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mensaje);
        }
        if (mensaje.startsWith("403")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mensaje);
        }
        if (mensaje.startsWith("404")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensaje);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("500: Error interno del servidor");
    }
}
