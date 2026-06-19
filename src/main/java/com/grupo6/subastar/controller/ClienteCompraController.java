package com.grupo6.subastar.controller;

import com.grupo6.subastar.dto.ModalidadEntregaRequest;
import com.grupo6.subastar.dto.PagarCompraRequest;
import com.grupo6.subastar.service.CompraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/clientes/me/compras")
public class ClienteCompraController {

    private final CompraService compraService;

    public ClienteCompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping("/{compraId}")
    public ResponseEntity<?> obtenerCompra(@PathVariable Integer compraId) {
        try {
            return ResponseEntity.ok(
                    compraService.obtenerCompra(emailAutenticado(), compraId));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    @PatchMapping("/{compraId}/entrega")
    public ResponseEntity<?> definirEntrega(
            @PathVariable Integer compraId,
            @RequestBody ModalidadEntregaRequest request) {
        try {
            return ResponseEntity.ok(compraService.definirEntrega(
                    emailAutenticado(),
                    compraId,
                    request == null ? null : request.getModalidad()));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    @PostMapping("/{compraId}/pagar")
    public ResponseEntity<?> pagar(
            @PathVariable Integer compraId,
            @RequestBody PagarCompraRequest request) {
        try {
            return ResponseEntity.ok(compraService.pagar(
                    emailAutenticado(),
                    compraId,
                    request == null ? null : request.getMedioPagoId()));
        } catch (RuntimeException e) {
            return manejarExcepcion(e);
        }
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ResponseEntity<String> manejarExcepcion(RuntimeException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        if (mensaje.startsWith("400")) return ResponseEntity.badRequest().body(mensaje);
        if (mensaje.startsWith("401")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mensaje);
        }
        if (mensaje.startsWith("403")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mensaje);
        }
        if (mensaje.startsWith("404")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensaje);
        }
        if (mensaje.startsWith("409")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("500: Error interno del servidor");
    }
}
