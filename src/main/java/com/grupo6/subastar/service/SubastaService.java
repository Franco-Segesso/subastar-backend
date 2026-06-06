package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.PujaRequest;
import com.grupo6.subastar.model.*;
import com.grupo6.subastar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubastaService {

    @Autowired
    private SubastaRepository subastaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PujaRepository pujaRepository;
    @Autowired
    private ItemCatalogoRepository itemCatalogoRepository;
    @Autowired
    private AsistenteRepository asistenteRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void ingresarSala(Integer subastaId, String emailUsuario) {
        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("404: Cliente no encontrado"));
        
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RuntimeException("404: Subasta no encontrada"));


        int pesoCliente = obtenerPesoCategoria(cliente.getCategoria());
        int pesoSubasta = obtenerPesoCategoria(subasta.getCategoria());

        if (pesoCliente < pesoSubasta) {
            throw new RuntimeException("403: Tu categoría (" + cliente.getCategoria() + ") no es suficiente para participar en esta subasta (" + subasta.getCategoria() + ")");
        }

        // Regla: Control de concurrencia
        if (asistenteRepository.existsByClienteAndActivo(cliente, "si")) {
            throw new RuntimeException("400: El usuario ya está conectado en otra subasta");
        }

        Asistente nuevoAsistente = new Asistente();
        nuevoAsistente.setCliente(cliente);
        nuevoAsistente.setSubasta(subasta);
        nuevoAsistente.setActivo("si");
        nuevoAsistente.setFechaIngreso(LocalDateTime.now());
        // Simulación temporal de asignación de paleta/número postor
        nuevoAsistente.setNumeroPostor((int) (Math.random() * 1000));
        
        asistenteRepository.save(nuevoAsistente);
    }

    @Transactional
    public void salirSala(Integer subastaId, String emailUsuario) {
        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario).orElseThrow();
        Asistente asistente = asistenteRepository.findByClienteAndSubastaIdAndActivo(cliente, subastaId, "si")
                .orElseThrow(() -> new RuntimeException("404: El cliente no está conectado a esta subasta"));
        
        asistente.setActivo("no");
        asistenteRepository.save(asistente);
    }

    @Transactional
    public Object procesarPuja(Integer subastaId, PujaRequest request, String emailUsuario) {
        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario).orElseThrow();
        
        ItemCatalogo item = itemCatalogoRepository.findByIdAndSubastaId(subastaId, request.getItemId())
                .orElseThrow(() -> new RuntimeException("404: Ítem no encontrado o no pertenece a la subasta"));
                
        Asistente asistente = asistenteRepository.findByClienteAndSubastaIdAndActivo(cliente, subastaId, "si")
                .orElseThrow(() -> new RuntimeException("403: Modo observador o no ingresado en sala"));

        // Obtener puja más alta
        Optional<Puja> pujaMaximaOpt = pujaRepository.findTopByItemCatalogoOrderByImporteDesc(item);
        Double valorReferencia = pujaMaximaOpt.isPresent() ? pujaMaximaOpt.get().getImporte() : item.getPrecioBase();
        
        // Reglas de cátedra: Mínimo = Mejor valor actual + 1% del VALOR BASE
        // Máximo = Mejor valor actual + 20% del VALOR BASE
        Double limiteMinimo = valorReferencia + (item.getPrecioBase() * 0.01);
        Double limiteMaximo = valorReferencia + (item.getPrecioBase() * 0.20);

        if (request.getImporte() < limiteMinimo) {
            throw new RuntimeException("400: Importe por debajo del mínimo permitido");
        }

        String cat = cliente.getCategoria();
        if (cat == null) cat = "comun"; // Manejo de fallback por BD
        
        if (!cat.equalsIgnoreCase("oro") && !cat.equalsIgnoreCase("platino")) {
            if (request.getImporte() > limiteMaximo) {
                throw new RuntimeException("409: Importe supera el máximo permitido");
            }
        }

        // Crear Puja mapeada a tu entidad exacta
        Puja nuevaPuja = new Puja();
        nuevaPuja.setAsistente(asistente);
        nuevaPuja.setItemCatalogo(item); // Tu entidad usa setItemCatalogo
        nuevaPuja.setImporte(request.getImporte());
        nuevaPuja.setFechaHora(LocalDateTime.now());
        nuevaPuja.setGanador("no");
        
        pujaRepository.save(nuevaPuja);

        // Emitir evento WebSocket
        messagingTemplate.convertAndSend("/topic/subastas/" + subastaId, nuevaPuja);

        return nuevaPuja; 
    }

    private int obtenerPesoCategoria(String categoria) {
        if (categoria == null) return 1; // Por defecto asumimos la más baja
        switch (categoria.toLowerCase()) {
            case "platino": return 5;
            case "oro": return 4;
            case "plata": return 3;
            case "especial": return 2;
            case "comun": default: return 1;
        }
    }
}