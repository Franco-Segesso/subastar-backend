package com.grupo6.subastar.controller;

import com.grupo6.subastar.dto.*;
import com.grupo6.subastar.model.MedioPago;
import com.grupo6.subastar.service.MedioPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medios-pago")
public class MedioPagoController {

    @Autowired
    private MedioPagoService medioPagoService;

    // GET /v1/medios-pago/cliente/{clienteId}
    // Devuelve todos los medios de pago activos de un cliente
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<?> obtenerMediosPago(@PathVariable Integer clienteId) {
        try {
            List<MedioPago> medios = medioPagoService.obtenerMediosPago(clienteId);
            return ResponseEntity.ok(medios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // POST /v1/medios-pago/tarjeta/{clienteId}
    @PostMapping("/tarjeta/{clienteId}")
    public ResponseEntity<?> agregarTarjeta(
            @PathVariable Integer clienteId,
            @RequestBody AgregarTarjetaRequest request) {
        try {
            MedioPago resultado = medioPagoService.agregarTarjeta(clienteId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // POST /v1/medios-pago/cuenta/{clienteId}
    @PostMapping("/cuenta/{clienteId}")
    public ResponseEntity<?> agregarCuenta(
            @PathVariable Integer clienteId,
            @RequestBody AgregarCuentaRequest request) {
        try {
            MedioPago resultado = medioPagoService.agregarCuenta(clienteId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // POST /v1/medios-pago/cheque/{clienteId}
    @PostMapping("/cheque/{clienteId}")
    public ResponseEntity<?> agregarCheque(
            @PathVariable Integer clienteId,
            @RequestBody AgregarChequeRequest request) {
        try {
            MedioPago resultado = medioPagoService.agregarCheque(clienteId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // DELETE /v1/medios-pago/{id}
   
    @DeleteMapping("/{id}")
    public ResponseEntity<?> darDeBaja(@PathVariable Integer id) {
        try {
            medioPagoService.darDeBaja(id);
            return ResponseEntity.ok("{\"mensaje\": \"Medio de pago dado de baja correctamente.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}