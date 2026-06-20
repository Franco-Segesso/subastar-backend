package com.grupo6.subastar.controller;

import com.grupo6.subastar.dto.PagarMultaRequest;
import com.grupo6.subastar.service.MultaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes/me/multas")
public class MultaController {

    private final MultaService multaService;

    public MultaController(MultaService multaService) {
        this.multaService = multaService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(multaService.listar(emailAutenticado()));
        } catch (RuntimeException e) {
            return manejar(e);
        }
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> pagar(
            @PathVariable Integer id,
            @RequestBody PagarMultaRequest request) {
        try {
            return ResponseEntity.ok(multaService.pagar(
                    emailAutenticado(),
                    id,
                    request == null ? null : request.getMedioPagoId()));
        } catch (RuntimeException e) {
            return manejar(e);
        }
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ResponseEntity<String> manejar(RuntimeException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        if (mensaje.startsWith("400")) return ResponseEntity.badRequest().body(mensaje);
        if (mensaje.startsWith("401")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mensaje);
        if (mensaje.startsWith("404")) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensaje);
        if (mensaje.startsWith("409")) return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("500: Error interno del servidor");
    }
}
