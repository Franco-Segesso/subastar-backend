package com.grupo6.subastar.controller;

import com.grupo6.subastar.dto.CuentaDestinoRequest;
import com.grupo6.subastar.dto.RespuestaConsignacionRequest;
import com.grupo6.subastar.service.ConsignacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/consignaciones")
public class ConsignacionController {

    @Autowired
    private ConsignacionService consignacionService;

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(consignacionService.listar(emailAutenticado()));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestParam("tipoBien") String tipoBien,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "artista", required = false) String artista,
            @RequestParam(value = "fechaCreacion", required = false) String fechaCreacion,
            @RequestParam(value = "historia", required = false) String historia,
            @RequestParam("declaraPropiedad") Boolean declaraPropiedad,
            @RequestPart("fotos") List<MultipartFile> fotos) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consignacionService.crear(
                    emailAutenticado(), tipoBien, descripcion, artista, fechaCreacion, historia, declaraPropiedad, fotos));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(consignacionService.obtenerDetalle(emailAutenticado(), id));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    @PostMapping("/{id}/cuenta-destino")
    public ResponseEntity<?> cuentaDestino(@PathVariable Integer id, @RequestBody CuentaDestinoRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consignacionService.registrarCuentaDestino(emailAutenticado(), id, request));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    @PostMapping(value = "/{id}/documentacion-origen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> documentacionOrigen(
            @PathVariable Integer id,
            @RequestPart("archivos") List<MultipartFile> archivos,
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consignacionService.registrarDocumentacion(emailAutenticado(), id, archivos, descripcion));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    @PatchMapping("/{id}/respuesta")
    public ResponseEntity<?> responder(@PathVariable Integer id, @RequestBody RespuestaConsignacionRequest request) {
        try {
            return ResponseEntity.ok(consignacionService.responderCondiciones(emailAutenticado(), id, request));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ResponseEntity<?> manejarExcepciones(RuntimeException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.startsWith("400")) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        if (msg.startsWith("401")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
        if (msg.startsWith("403")) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
        if (msg.startsWith("404")) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        if (msg.startsWith("409")) return ResponseEntity.status(HttpStatus.CONFLICT).body(msg);
        if (msg.startsWith("422")) return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("500: Error interno del servidor");
    }
}
