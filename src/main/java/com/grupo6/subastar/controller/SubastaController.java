package com.grupo6.subastar.controller;
import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.model.Duenio;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.repository.SubastaRepository;
import com.grupo6.subastar.service.SubastaService;
import com.grupo6.subastar.repository.DuenioRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import com.grupo6.subastar.repository.PujaRepository;
import com.grupo6.subastar.repository.SeguroRepository;
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
    private ItemCatalogoRepository itemCatalogoRepository;

    @Autowired
    private DuenioRepository duenioRepository;

    @Autowired
    private SeguroRepository seguroRepository;

    @Autowired
    private SubastaService subastaService;


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


        // Si el usuario es invitado (no hay token), se ocultan los precios base de los catálogos
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

        // Ocultar precios base a usuarios no autenticados
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
            String nroPoliza = item.getProducto().getSeguro();
            if (nroPoliza != null && !nroPoliza.isBlank()) {
                item.setSeguroDetalle(seguroRepository.findById(nroPoliza).orElse(null));
            }
        }

        // Ocultar precio base si no se provee token
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


    // 4. POST /subastas/{id}/ingresar
    @PostMapping("/{id}/ingresar")
    public ResponseEntity<?> ingresarSubasta(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization") String token) {
        try {
            
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            subastaService.ingresarSala(id, email);
            return ResponseEntity.ok().build(); // 200 OK
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    // 5. POST /subastas/{id}/salir
    @PostMapping("/{id}/salir")
    public ResponseEntity<?> salirSubasta(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization") String token) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            subastaService.salirSala(id, email);
            return ResponseEntity.ok().build(); 
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    // 6. POST /subastas/{id}/pujas
    @PostMapping("/{id}/pujas")
    public ResponseEntity<?> registrarPuja(
            @PathVariable Integer id,
            @RequestBody com.grupo6.subastar.dto.PujaRequest request,
            @RequestHeader(value = "Authorization") String token) {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            Object response = subastaService.procesarPuja(id, request, email);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response); // 201 Created
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    // 7. GET /subastas/{id}/pujas/{itemId}
    @GetMapping("/{id}/pujas/{itemId}")
    public ResponseEntity<?> listarPujasItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestHeader(value = "Authorization") String token) {
        try {
            return ResponseEntity.ok(subastaService.listarPujasItem(id, itemId));
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }

    
    @PostMapping("/{id}/items/{itemId}/cerrar")
    public ResponseEntity<?> cerrarSubasta(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestHeader(value = "Authorization") String token) {
        try {
            com.grupo6.subastar.dto.CierreSubastaDTO response = subastaService.cerrarSubastaItem(id, itemId);
            return ResponseEntity.ok(response); 
        } catch (RuntimeException e) {
            return manejarExcepciones(e);
        }
    }
    

    
    private ResponseEntity<?> manejarExcepciones(RuntimeException e) {
        String msg = e.getMessage();
        if (msg.startsWith("400")) return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(msg);
        if (msg.startsWith("403")) return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(msg);
        if (msg.startsWith("404")) return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(msg);
        if (msg.startsWith("409")) return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(msg);
        if (msg.startsWith("422")) return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY).body(msg);
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("500: Error interno del servidor");
    }
}

