package com.grupo6.subastar.controller;

import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.repository.SubastaRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/subastas")
public class SubastaController {

    @Autowired
    private SubastaRepository subastaRepository;

    @Autowired
    private ItemCatalogoRepository itemCatalogoRepository;

    // 1. GET /v1/subastas (Listado general con filtros opcionales)
    @GetMapping
    public ResponseEntity<List<Subasta>> listarSubastas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            @RequestHeader(value = "Authorization", required = false) String token) {

        List<Subasta> subastas = subastaRepository.findByFiltros(estado, categoria, moneda, fecha);
        
        // Regra de negocio: Si el usuario es invitado (no hay token), se ocultan los precios base de los catálogos
        if (token == null || token.isEmpty()) {
            subastas.forEach(s -> {
                if (s.getCatalogo() != null && s.getCatalogo().getItems() != null) {
                    s.getCatalogo().getItems().forEach(item -> item.setPrecioBase(null));
                }
            });
        }

        return ResponseEntity.ok(subastas);
    }

    // 2. GET /v1/subastas/{id} (Detalle de una subasta específica y su catálogo)
    @GetMapping("/{id}")
    public ResponseEntity<Subasta> obtenerDetalleSubasta(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Optional<Subasta> subastaOpt = subastaRepository.findById(id);

        if (subastaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Subasta subasta = subastaOpt.get();

        // Regla de negocio: Ocultar precios base a usuarios no autenticados
        if (token == null || token.isEmpty()) {
            if (subasta.getCatalogo() != null && subasta.getCatalogo().getItems() != null) {
                subasta.getCatalogo().getItems().forEach(item -> item.setPrecioBase(null));
            }
        }

        return ResponseEntity.ok(subasta);
    }

    // 3. GET /v1/subastas/{id}/items/{itemId} (Detalle de un artículo concreto del catálogo)
    @GetMapping("/{id}/items/{itemId}")
    public ResponseEntity<ItemCatalogo> obtenerDetalleItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Optional<ItemCatalogo> itemOpt = itemCatalogoRepository.findByIdAndSubastaId(id, itemId);

        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ItemCatalogo item = itemOpt.get();

        // Regla de negocio: Ocultar precio base si no se provee token
        if (token == null || token.isEmpty()) {
            item.setPrecioBase(null);
        }

        return ResponseEntity.ok(item);
    }
}