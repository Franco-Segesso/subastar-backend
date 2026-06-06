package com.grupo6.subastar.controller;

import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.model.Duenio;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.repository.SubastaRepository;
import com.grupo6.subastar.repository.DuenioRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import com.grupo6.subastar.repository.PersonaRepository;
import com.grupo6.subastar.repository.PujaRepository;

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
    private PujaRepository pujaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ItemCatalogoRepository itemCatalogoRepository;

    @Autowired
    private DuenioRepository duenioRepository;


    // 1. GET /v1/subastas (Listado general con filtros opcionales)
    @GetMapping
    public ResponseEntity<List<Subasta>> listarSubastas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            @RequestHeader(value = "Authorization", required = false) String token) {

        List<Subasta> subastas = subastaRepository.findByFiltros(estado, categoria, moneda, fecha);
        
        for (Subasta s : subastas) {
            // 1. Calcular la mejor oferta
            Double maxOferta = pujaRepository.findMaxImporteBySubastaId(s.getId());
            s.setMejorOferta(maxOferta != null ? maxOferta : 0.0);

            // 2. Calcular los postores únicos
            Integer postores = pujaRepository.countDistinctPostoresBySubastaId(s.getId());
            s.setCantidadPostores(postores != null ? postores : 0);
        }


        // Regra de negocio: Si el usuario es invitado (no hay token), se ocultan los precios base de los catálogos
        if (token == null || token.isEmpty()) {
            subastas.forEach(s -> {

                s.setMejorOferta(null); // Oculta la mejor oferta para usuarios no autenticados

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

        if (item != null && item.getProducto() != null) {
        asignarNombreDuenio(item.getProducto());
    }

        // Regla de negocio: Ocultar precio base si no se provee token
        if (token == null || token.isEmpty()) {
            item.setPrecioBase(null);
        }

        return ResponseEntity.ok(item);
    }


    private void asignarNombreDuenio(Producto p) {
        if (p != null && p.getDuenio() != null) {
            // Buscamos el dueño en la tabla duenios
            Duenio duenioObj = duenioRepository.findById(p.getDuenio()).orElse(null);
            
            // Si existe y tiene persona, le asignamos el nombre completo
            if (duenioObj != null && duenioObj.getPersona() != null) {
                Persona persona = duenioObj.getPersona();
                p.setNombreDuenioReal(persona.getNombre() + " " + persona.getApellido());
            } else {
                p.setNombreDuenioReal("Dueño Desconocido");
            }
        }
    }
}

