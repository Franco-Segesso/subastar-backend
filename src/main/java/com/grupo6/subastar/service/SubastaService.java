package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.CierreSubastaDTO;
import com.grupo6.subastar.dto.EstadoPujaDTO;
import com.grupo6.subastar.dto.EstadoSubastaDTO;
import com.grupo6.subastar.dto.PujaMensajeDTO;
import com.grupo6.subastar.dto.PujaRequest;
import com.grupo6.subastar.model.*;
import com.grupo6.subastar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    @Autowired
    private RegistroSubastaRepository registroSubastaRepository;
    @Autowired
    private MedioPagoService medioPagoService;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private MultaService multaService;
    @Autowired
    private FirebasePushService firebasePushService;
    @Autowired
    private NotificacionesReactivasService notificacionesReactivasService;
    @Autowired
    private SolicitudConsignacionRepository solicitudConsignacionRepository;
    @Autowired
    private DuenioRepository duenioRepository;
    @Autowired
    private CuentaDestinoRepository cuentaDestinoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CompraService compraService;

    private static final long DURACION_ITEM_SEGUNDOS = 60;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String EMAIL_CLIENTE_EMPRESA = "empresa@subastar.com";

    private final Map<Integer, EstadoItemActivo> itemsActivos = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public synchronized void procesarRelojSubastas() {
        LocalDateTime ahora = ahoraNegocio();
        String fechaArgentina = ahora.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        LocalTime horaArgentina = ahora.toLocalTime();
        String horaArgentinaSql = horaArgentina.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME);

        for (EstadoItemActivo estado : List.copyOf(itemsActivos.values())) {
            if (!estado.cerrando && !estado.deadline.isAfter(ahora)) {
                if (extenderDeadlineConUltimaPuja(estado, ahora)) {
                    continue;
                }
                estado.cerrando = true;
                try {
                    cerrarSubastaItem(estado.subastaId, estado.itemId);
                    activarSiguienteItem(estado.subastaId, ahoraNegocio());
                } catch (RuntimeException e) {
                    estado.cerrando = false;
                    System.err.println(">> ERROR cerrando item " + estado.itemId + " de subasta " + estado.subastaId + ": " + e.getMessage());
                }
            }
        }

        for (Subasta subasta : subastaRepository.findPendientesParaAbrir(fechaArgentina, horaArgentinaSql)) {
            subastaRepository.actualizarEstado(subasta.getId(), "abierta");
            emitirEstadoSubasta(subasta.getId(), "abierta");
            System.out.println(">> SUBASTA " + subasta.getId() + " abierta automaticamente a las " + ahora + " (" + ZONA_NEGOCIO + ")");
            activarSiguienteItem(subasta.getId(), ahora);
        }

        for (Subasta subasta : subastaRepository.findAll()) {
            if ("abierta".equals(normalizar(subasta.getEstado())) && !itemsActivos.containsKey(subasta.getId())) {
                activarSiguienteItem(subasta.getId(), ahora);
            }
        }

        for (EstadoItemActivo estado : List.copyOf(itemsActivos.values())) {
            if (!estado.cerrando) {
                emitirEstado(estado, ahora, false);
            }
        }
    }

    @Transactional
    public void ingresarSala(
            Integer subastaId,
            String emailUsuario,
            boolean soloObservar) {
        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("404: Cliente no encontrado"));
        
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RuntimeException("404: Subasta no encontrada"));

        if (!"abierta".equals(normalizar(subasta.getEstado()))) {
            throw new RuntimeException("409: Subasta cerrada");
        }

        // FASE 2: Validaciones estrictas bloqueadas si solo viene a mirar
        if (!soloObservar) {
            // Validamos capacidad de la sala
            int capacidadMaxima = (subasta.getCapacidadAsistentes() != null) ? subasta.getCapacidadAsistentes() : 50;
            int postoresActivos = asistenteRepository.countPostoresActivos(subastaId);
            
            if (postoresActivos >= capacidadMaxima) {
                throw new RuntimeException("409: La sala ha alcanzado su capacidad máxima de " + capacidadMaxima + " postores.");
            }

            //validamos que no tenga multas pendientes que le impidan participar
            multaService.validarPuedeParticipar(cliente);

            int pesoCliente = obtenerPesoCategoria(cliente.getCategoria());
            int pesoSubasta = obtenerPesoCategoria(subasta.getCategoria());

            //validamos que la categoría del cliente sea suficiente para participar en la subasta
            if (pesoCliente < pesoSubasta) {
                throw new RuntimeException("403: Tu categoría (" + cliente.getCategoria() + ") no es suficiente para participar en esta subasta (" + subasta.getCategoria() + ")");
            }
            
        }

        EstadoItemActivo estadoActivo = itemsActivos.get(subastaId);
        if (estadoActivo == null) {
            activarSiguienteItem(subastaId, ahoraNegocio());
            estadoActivo = itemsActivos.get(subastaId);
        }

        // FASE 2: Evitamos validar si es su propio ítem si solo viene a observar
        if (estadoActivo != null && !soloObservar) {
            ItemCatalogo itemActivo = itemCatalogoRepository
                    .findByIdAndSubastaId(subastaId, estadoActivo.itemId)
                    .orElse(null);
            if (itemActivo != null
                    && itemActivo.getProducto() != null
                    && cliente.getIdentificador().equals(
                            itemActivo.getProducto().getDuenio())) {
                throw new RuntimeException(
                        "403: No podes participar en la puja de un item propio");
            }
        }

        asistenteRepository.desactivarSesionesFueraDeSubastaAbierta(cliente);

        Optional<Asistente> asistenteActual = asistenteRepository.findByClienteAndSubastaIdAndActivo(cliente, subastaId, "si");
        if (asistenteActual.isPresent()) {
            emitirEstadoActual(subastaId);
            return;
        }
        
        if (asistenteRepository.existsByClienteAndActivo(cliente, "si")) {
            throw new RuntimeException("400: El usuario ya está conectado en otra subasta");
        }

        Asistente nuevoAsistente = new Asistente();
        nuevoAsistente.setCliente(cliente);
        nuevoAsistente.setSubasta(subasta);
        nuevoAsistente.setActivo("si");
        nuevoAsistente.setFechaIngreso(ahoraNegocio());

        //Si el cliente entra como observador, le damos el número -1. Si no, un número aleatorio para identificarlo en la sala.
        if (soloObservar) {
            nuevoAsistente.setNumeroPostor(-1);
        } else {
            nuevoAsistente.setNumeroPostor((int) (Math.random() * 1000) + 1); // +1 para evitar ceros
        }

        
        asistenteRepository.save(nuevoAsistente);
        emitirEstadoActual(subastaId);
    }

    @Transactional
    public void salirSala(Integer subastaId, String emailUsuario) {
        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario).orElseThrow();
        Asistente asistente = asistenteRepository.findByClienteAndSubastaIdAndActivo(cliente, subastaId, "si")
                .orElseThrow(() -> new RuntimeException("404: El cliente no está conectado a esta subasta"));
        
        EstadoItemActivo estadoActivo = itemsActivos.get(subastaId);
        if (estadoActivo != null) {
            ItemCatalogo itemActivo = itemCatalogoRepository.findByIdAndSubastaId(subastaId, estadoActivo.itemId)
                    .orElse(null);
            if (itemActivo != null) {
                Optional<Puja> pujaMaxima = pujaRepository.findTopByItemCatalogoOrderByImporteDesc(itemActivo);
                if (pujaMaxima.isPresent()) {
                    Cliente clienteMayorPostor = pujaMaxima.get().getAsistente().getCliente();
                    if (clienteMayorPostor != null && cliente.getIdentificador().equals(clienteMayorPostor.getIdentificador())) {
                        throw new RuntimeException("409: No podes salir mientras sos el mayor postor de este item");
                    }
                }
            }
        }

        asistente.setActivo("no");
        asistenteRepository.save(asistente);
    }

    @Transactional
    public synchronized PujaMensajeDTO procesarPuja(Integer subastaId, PujaRequest request, String emailUsuario) {
        if (request == null || request.getItemId() == null
                || request.getImporte() == null
                || request.getMedioPagoId() == null
                || request.getModalidadEntrega() == null) {
            throw new RuntimeException(
                    "400: Debe indicar itemId, importe, medioPagoId "
                            + "y modalidadEntrega");
        }
        String modalidadEntrega = normalizar(
                request.getModalidadEntrega());
        if (!"envio".equals(modalidadEntrega)
                && !"retiro".equals(modalidadEntrega)) {
            throw new RuntimeException(
                    "400: Modalidad de entrega invalida");
        }

        EstadoItemActivo estadoActivo = itemsActivos.get(subastaId);
        if (estadoActivo == null) {
            Subasta subasta = subastaRepository.findById(subastaId)
                    .orElseThrow(() -> new RuntimeException("404: Subasta no encontrada"));
            if (!"abierta".equals(normalizar(subasta.getEstado()))) {
                throw new RuntimeException("409: Subasta cerrada o pendiente");
            }
            activarSiguienteItem(subastaId, ahoraNegocio());
            estadoActivo = itemsActivos.get(subastaId);
        }

        if (estadoActivo == null || !request.getItemId().equals(estadoActivo.itemId)) {
            throw new RuntimeException("422: El item no es el item activo de la subasta");
        }

        LocalDateTime ahoraPuja = ahoraNegocio();
        if (!estadoActivo.deadline.isAfter(ahoraPuja)) {
            estadoActivo.cerrando = true;
            cerrarSubastaItem(subastaId, estadoActivo.itemId);
            activarSiguienteItem(subastaId, ahoraNegocio());
            throw new RuntimeException("422: El item ya finalizo");
        }

        Cliente cliente = clienteRepository.findByPersonaEmail(emailUsuario).orElseThrow();
        multaService.validarPuedeParticipar(cliente);
        
        ItemCatalogo item = itemCatalogoRepository.findByIdAndSubastaId(subastaId, request.getItemId())
                .orElseThrow(() -> new RuntimeException("404: Ítem no encontrado o no pertenece a la subasta"));
        if ("si".equals(normalizar(item.getSubastado()))) {
            throw new RuntimeException("422: El item ya fue subastado");
        }

        Asistente asistente = asistenteRepository.findByClienteAndSubastaIdAndActivo(cliente, subastaId, "si")
                .orElseThrow(() -> new RuntimeException("403: Modo observador o no ingresado en sala"));

        // Bloqueo de seguridad para evitar que un cliente pueda pujar estando en modo observador.
        if (asistente.getNumeroPostor() != null && asistente.getNumeroPostor() == -1) {
            throw new RuntimeException("403: Estás en modo observador, no podés pujar.");
        }

        // Obtener puja más alta
        Optional<Puja> pujaMaximaOpt = pujaRepository.findTopByItemCatalogoOrderByImporteDesc(item);
        Double valorReferencia = pujaMaximaOpt.isPresent() ? pujaMaximaOpt.get().getImporte() : item.getPrecioBase();
        
        // Mínimo = Mejor valor actual + 1% del VALOR BASE
        // Máximo = Mejor valor actual + 20% del VALOR BASE
        Double limiteMinimo = valorReferencia + (item.getPrecioBase() * 0.01);
        Double limiteMaximo = valorReferencia + (item.getPrecioBase() * 0.20);

        if (request.getImporte() < limiteMinimo) {
            throw new RuntimeException("400: Importe por debajo del mínimo permitido");
        }

        String cat = cliente.getCategoria();
        if (cat == null) cat = "comun"; 
        
        if (!cat.equalsIgnoreCase("oro") && !cat.equalsIgnoreCase("platino")) {
            if (request.getImporte() > limiteMaximo) {
                throw new RuntimeException("409: Importe supera el máximo permitido");
            }
        }

        medioPagoService.validarParaPuja(
                request.getMedioPagoId(),
                cliente,
                item.getCatalogo().getSubasta().getMoneda());

        // Crear Puja mapeada a tu entidad exacta
        Puja nuevaPuja = new Puja();
        nuevaPuja.setAsistente(asistente);
        nuevaPuja.setItemCatalogo(item); // Tu entidad usa setItemCatalogo
        nuevaPuja.setImporte(request.getImporte());
        nuevaPuja.setFechaHora(ahoraNegocio());
        nuevaPuja.setGanador("no");
        nuevaPuja.setMedioPagoId(request.getMedioPagoId());
        nuevaPuja.setModalidadEntrega(modalidadEntrega);
        
        pujaRepository.save(nuevaPuja);

        

        // ENVÍO LIMPIO POR WEBSOCKET PARA EVITAR BUCLE INFINITO
        PujaMensajeDTO mensajeLimpio = convertirPujaADto(nuevaPuja);

        // Emitir evento WebSocket usando el Map limpio
        messagingTemplate.convertAndSend("/topic/subastas/" + subastaId, mensajeLimpio);

        estadoActivo.deadline = ahoraNegocio().plusSeconds(DURACION_ITEM_SEGUNDOS);
        estadoActivo.importeActual = nuevaPuja.getImporte();
        emitirEstado(estadoActivo, ahoraNegocio(), false);

        return mensajeLimpio;
    }

    @Transactional(readOnly = true)
    public List<PujaMensajeDTO> listarPujasItem(Integer subastaId, Integer itemId) {
        ItemCatalogo item = itemCatalogoRepository.findByIdAndSubastaId(subastaId, itemId)
                .orElseThrow(() -> new RuntimeException("404: Item no encontrado o no pertenece a la subasta"));

        return pujaRepository.findByItemCatalogoOrderByFechaHoraDesc(item)
                .stream()
                .map(this::convertirPujaADto)
                .collect(Collectors.toList());
    }

    @Transactional
    public synchronized CierreSubastaDTO cerrarSubastaItem(Integer subastaId, Integer itemId) {
        ItemCatalogo item = itemCatalogoRepository.findByIdAndSubastaId(subastaId, itemId)
                .orElseThrow(() -> new RuntimeException("404: Ítem no encontrado o no pertenece a la subasta"));

        if ("si".equals(normalizar(item.getSubastado()))) {
            throw new RuntimeException("409: La subasta de este ítem ya fue cerrada previamente");
        }

        Optional<Puja> pujaGanadoraOpt = pujaRepository.findTopByItemCatalogoOrderByImporteDesc(item);
        
        CierreSubastaDTO respuesta = new CierreSubastaDTO();
        respuesta.setItemId(itemId);
        Runnable notificacionPostCommit = null;

        if (pujaGanadoraOpt.isPresent()) {
            // Se vendió
            Puja ganadora = pujaGanadoraOpt.get();
            ganadora.setGanador("si"); 
            pujaRepository.save(ganadora);
            
            item.setSubastado("si"); 
            item.setPrecioFinal(ganadora.getImporte()); // Guardamos el monto final
            RegistroSubasta compra = registrarCompraSiNoExiste(subastaId, item, ganadora);

            respuesta.setHayGanador(true);
            respuesta.setIdClienteGanador(ganadora.getAsistente().getCliente().getIdentificador());
            respuesta.setImporteFinal(ganadora.getImporte());
            respuesta.setCompraId(compra.getIdentificador());

            boolean pagoAutomatico =
                    compraService.intentarPagoAutomatico(compra);
            respuesta.setPagoAutomatico(pagoAutomatico);
            respuesta.setMultaGenerada(!pagoAutomatico);
            if (pagoAutomatico) {
                respuesta.setMensajePago(
                        "La compra se cobro automaticamente con el medio "
                                + "seleccionado.");
            } else {
                multaService.generarPorFondosInsuficientes(
                        ganadora, ahoraNegocio());
                respuesta.setMensajePago(
                        "El medio seleccionado no tenia fondos suficientes. "
                                + "Se genero una multa del 10%.");
            }

            Cliente clienteGanador = ganadora.getAsistente().getCliente();
            //evaluamos si el cliente ganador sube de categoría por esta compra
            medioPagoService.evaluarYActualizarCategoria(clienteGanador.getIdentificador());
            
            Integer consignacionId = solicitudConsignacionRepository
                    .findIdByProductoId(item.getProducto().getId())
                    .orElse(null);
            if (consignacionId != null) {
                solicitudConsignacionRepository.findById(consignacionId)
                        .ifPresent(solicitud -> {
                            solicitud.setEstado("vendida");
                            solicitudConsignacionRepository.save(solicitud);
                        });
            }
        } else {
    // No hubo pujas: la empresa compra el ítem al precio base.
    item.setSubastado("si");
    item.setPrecioFinal(item.getPrecioBase());

    RegistroSubasta compraEmpresa = registrarCompraEmpresaSiNoExiste(subastaId, item);

    respuesta.setHayGanador(false);
    respuesta.setIdClienteGanador(compraEmpresa.getClienteId());
    respuesta.setImporteFinal(item.getPrecioBase());
    respuesta.setCompraId(compraEmpresa.getIdentificador());

    double valorVenta = item.getPrecioBase();
    double comisiones = calcularComision(valorVenta, item.getComision());

    notificarVentaSinInterrumpirCierre(
            item.getProducto().getDuenio(),
            item.getProducto().getDescripcion(),
            valorVenta,
            comisiones,
            solicitudConsignacionRepository
                    .findIdByProductoId(item.getProducto().getId())
                    .orElse(null));
}

        
        
        itemCatalogoRepository.saveAndFlush(item);

        // 
        // Contamos cuántos ítems de esta subasta todavía dicen subastado = "no"
        long itemsPendientes = itemCatalogoRepository.countPendientesBySubastaId(subastaId);
        
        if (itemsPendientes == 0) {
            
            Subasta subasta = subastaRepository.findById(subastaId)
                    .orElseThrow(() -> new RuntimeException("404: Subasta no encontrada"));
            subastaRepository.actualizarEstado(subasta.getId(), "cerrada");
            emitirEstadoSubasta(subasta.getId(), "cerrada");
            
            
        }

        
        itemsActivos.remove(subastaId);
        Runnable notificacionFinal = notificacionPostCommit;
        ejecutarDespuesDeCommit(() -> {
            messagingTemplate.convertAndSend(
                    "/topic/subastas/" + subastaId + "/cierre", respuesta);
            messagingTemplate.convertAndSend(
                    "/topic/subastas/" + subastaId + "/estado",
                    new EstadoPujaDTO(
                            subastaId, itemId, 0, respuesta.getImporteFinal(),
                            null, null, true));
            if (notificacionFinal != null) {
                notificacionFinal.run();
            }
        });

        return respuesta;
    }

    private RegistroSubasta registrarCompraSiNoExiste(
            Integer subastaId,
            ItemCatalogo item,
            Puja ganadora) {
        Producto producto = item.getProducto();
        Cliente cliente = ganadora.getAsistente().getCliente();
        if (producto == null || producto.getId() == null || cliente == null) {
            throw new RuntimeException("500: No se pudo registrar la compra ganadora");
        }

        Optional<RegistroSubasta> registrada = registroSubastaRepository
                .findFirstBySubastaIdAndProductoIdAndClienteId(
                        subastaId,
                        producto.getId(),
                        cliente.getIdentificador());
        if (registrada.isPresent()) return registrada.get();

        RegistroSubasta compra = new RegistroSubasta();
        compra.setSubastaId(subastaId);
        compra.setDuenioId(producto.getDuenio());
        compra.setProductoId(producto.getId());
        compra.setClienteId(cliente.getIdentificador());
        compra.setImporte(ganadora.getImporte());
        compra.setComision(calcularComision(
                ganadora.getImporte(), item.getComision()));
        String modalidad = normalizar(ganadora.getModalidadEntrega());
        compra.setCostoEnvio("envio".equals(modalidad) ? 5000.0 : 0.0);
        compra.setNroPolizaSeguro(producto.getSeguro());
        compra.setModalidadEntrega(modalidad);
        compra.setMedioPagoId(ganadora.getMedioPagoId());
        compra.setEstadoPago("pendiente");
        compra.setEstadoEntrega("pendiente");
        return registroSubastaRepository.save(compra);
    }

    private RegistroSubasta registrarCompraEmpresaSiNoExiste(
        Integer subastaId,
        ItemCatalogo item) {

    Producto producto = item.getProducto();

    if (producto == null || producto.getId() == null) {
        throw new RuntimeException("500: No se pudo registrar la compra de la empresa");
    }

    Cliente clienteEmpresa = clienteRepository.findByPersonaEmail(EMAIL_CLIENTE_EMPRESA)
            .orElseThrow(() -> new RuntimeException(
                    "500: No existe el cliente empresa con email " + EMAIL_CLIENTE_EMPRESA));

    Optional<RegistroSubasta> registrada = registroSubastaRepository
            .findFirstBySubastaIdAndProductoIdAndClienteId(
                    subastaId,
                    producto.getId(),
                    clienteEmpresa.getIdentificador());

    if (registrada.isPresent()) {
        return registrada.get();
    }

    Double importe = item.getPrecioBase();

    Integer duenioOriginal = producto.getDuenio();
    RegistroSubasta compra = new RegistroSubasta();
    compra.setSubastaId(subastaId);
    compra.setDuenioId(duenioOriginal);
    compra.setProductoId(producto.getId());
    compra.setClienteId(clienteEmpresa.getIdentificador());
    compra.setImporte(importe);
    compra.setComision(calcularComision(importe, item.getComision()));
    compra.setCostoEnvio(0.0);
    compra.setNroPolizaSeguro(producto.getSeguro());
    compra.setModalidadEntrega("retiro");
    compra.setMedioPagoId(null);
    compra.setEstadoPago("pagada");
    compra.setFechaPago(ahoraNegocio());
    compra.setEstadoEntrega("pendiente");
    compra.setFechaEntrega(null);
    compra = registroSubastaRepository.save(compra);
    double comisionEmpresa = valor(compra.getComision());

    asegurarDuenioEmpresa(clienteEmpresa);
    producto.setDuenio(clienteEmpresa.getIdentificador());
    producto.setDisponible("no");
    productoRepository.save(producto);

    solicitudConsignacionRepository.findByProductoId(producto.getId())
            .ifPresent(solicitud -> {
                solicitud.setEstado("vendida");
                solicitudConsignacionRepository.save(solicitud);
                String cbu = cuentaDestinoRepository
                        .findBySolicitudIdentificadorAndActiva(
                                solicitud.getIdentificador(), "si")
                        .map(CuentaDestino::getCbuIban)
                        .orElse(null);
                double neto = importe - comisionEmpresa;
                try {
                    notificacionesReactivasService.notificarTransferenciaEnviada(
                            duenioOriginal,
                            producto.getDescripcion(),
                            importe,
                            neto,
                            cbu,
                            solicitud.getIdentificador());
                } catch (RuntimeException e) {
                    System.err.println(
                            ">> La compra de la empresa se registro, pero no se "
                                    + "pudo notificar la transferencia: "
                                    + e.getMessage());
                }
            });

    return compra;
}

    private void asegurarDuenioEmpresa(Cliente clienteEmpresa) {
        if (duenioRepository.existsById(clienteEmpresa.getIdentificador())) {
            return;
        }
        Duenio duenio = new Duenio();
        duenio.setId(clienteEmpresa.getIdentificador());
        duenio.setPersona(clienteEmpresa.getPersona());
        duenio.setNumeroPais(clienteEmpresa.getPais() == null
                ? null : clienteEmpresa.getPais().getNumero());
        duenio.setCalificacionRiesgo(1);
        duenio.setVerificadorId(clienteEmpresa.getVerificadorId());
        duenioRepository.save(duenio);
    }

    private double calcularComision(Double importe, Double porcentaje) {
    if (importe == null || porcentaje == null) {
        return 0.0;
    }

    return importe * porcentaje / 100.0;
}

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }

    private void notificarVentaSinInterrumpirCierre(
            Integer duenioId,
            String descripcion,
            double importe,
            double comision,
            Integer referenciaId) {
        try {
            notificacionesReactivasService.notificarBienVendidoAlDuenio(
                    duenioId, descripcion, importe, comision, referenciaId);
        } catch (RuntimeException e) {
            System.err.println(
                    ">> El item se vendio a la empresa, pero no se pudo "
                            + "enviar la notificacion: " + e.getMessage());
        }
    }

    private void notificarSinInterrumpir(Runnable notificacion) {
        try {
            notificacion.run();
        } catch (RuntimeException e) {
            System.err.println(
                    ">> El cierre se confirmo, pero fallo una notificacion: "
                            + e.getMessage());
        }
    }

    private void ejecutarDespuesDeCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            accion.run();
                        }
                    });
        } else {
            accion.run();
        }
    }

    private void activarSiguienteItem(Integer subastaId, LocalDateTime ahora) {
        if (itemsActivos.containsKey(subastaId)) return;

        Subasta subasta = subastaRepository.findById(subastaId).orElse(null);
        if (subasta == null || !"abierta".equals(normalizar(subasta.getEstado()))) return;

        List<ItemCatalogo> pendientes = itemCatalogoRepository.findPendientesBySubastaId(subastaId);
        if (pendientes.isEmpty()) {
            subastaRepository.actualizarEstado(subasta.getId(), "cerrada");
            emitirEstadoSubasta(subasta.getId(), "cerrada");
            return;
        }

        ItemCatalogo item = pendientes.get(0);
        Optional<Puja> ultimaPuja =
                pujaRepository.findTopByItemCatalogoOrderByImporteDesc(item);
        Double importeActual = ultimaPuja
                .map(Puja::getImporte)
                .orElse(item.getPrecioBase());
        LocalDateTime deadline = ultimaPuja
                .map(Puja::getFechaHora)
                .filter(fecha -> fecha != null)
                .map(fecha -> fecha.plusSeconds(DURACION_ITEM_SEGUNDOS))
                .orElse(ahora.plusSeconds(DURACION_ITEM_SEGUNDOS));
        EstadoItemActivo estado = new EstadoItemActivo(
                subastaId,
                item.getId(),
                deadline,
                importeActual);
        itemsActivos.put(subastaId, estado);
        emitirEstado(estado, ahora, false);
    }

    private boolean extenderDeadlineConUltimaPuja(
            EstadoItemActivo estado,
            LocalDateTime ahora) {
        ItemCatalogo item = itemCatalogoRepository
                .findByIdAndSubastaId(estado.subastaId, estado.itemId)
                .orElse(null);
        if (item == null || "si".equals(normalizar(item.getSubastado()))) {
            return false;
        }
        Optional<Puja> ultimaPuja =
                pujaRepository.findTopByItemCatalogoOrderByImporteDesc(item);
        if (ultimaPuja.isEmpty() || ultimaPuja.get().getFechaHora() == null) {
            return false;
        }
        LocalDateTime deadlinePersistido = ultimaPuja.get().getFechaHora()
                .plusSeconds(DURACION_ITEM_SEGUNDOS);
        if (!deadlinePersistido.isAfter(estado.deadline)) {
            return false;
        }
        estado.deadline = deadlinePersistido;
        estado.importeActual = ultimaPuja.get().getImporte();
        emitirEstado(estado, ahora, false);
        return deadlinePersistido.isAfter(ahora);
    }

    private void emitirEstadoActual(Integer subastaId) {
        EstadoItemActivo estado = itemsActivos.get(subastaId);
        if (estado != null) {
            emitirEstado(estado, ahoraNegocio(), false);
        }
    }

    private void emitirEstado(EstadoItemActivo estado, LocalDateTime ahora, boolean cerrado) {
        long millisRestantes = Math.max(0, ChronoUnit.MILLIS.between(ahora, estado.deadline));
        long restante = (millisRestantes + 999) / 1000;
        ItemCatalogo item = itemCatalogoRepository
                .findByIdAndSubastaId(estado.subastaId, estado.itemId)
                .orElse(null);
        Double minimo = null;
        Double maximo = null;
        if (!cerrado && item != null && item.getPrecioBase() != null) {
            minimo = estado.importeActual + item.getPrecioBase() * 0.01;
            String categoria = item.getCatalogo().getSubasta().getCategoria();
            if (!"oro".equalsIgnoreCase(categoria)
                    && !"platino".equalsIgnoreCase(categoria)) {
                maximo = estado.importeActual + item.getPrecioBase() * 0.20;
            }
        }
        EstadoPujaDTO dto = new EstadoPujaDTO(
                estado.subastaId,
                estado.itemId,
                (int) restante,
                estado.importeActual,
                minimo,
                maximo,
                cerrado);
        messagingTemplate.convertAndSend("/topic/subastas/" + estado.subastaId + "/estado", dto);
    }

    private void emitirEstadoSubasta(Integer subastaId, String estado) {
        EstadoSubastaDTO dto = new EstadoSubastaDTO(subastaId, estado);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/subastas/estado-general", dto);
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/subastas/estado-general", dto);
        }
    }

    private LocalDateTime ahoraNegocio() {
        return LocalDateTime.now(ZONA_NEGOCIO);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase();
    }

    private int obtenerPesoCategoria(String categoria) {
        if (categoria == null) return 1; 
        switch (categoria.toLowerCase()) {
            case "platino": return 5;
            case "oro": return 4;
            case "plata": return 3;
            case "especial": return 2;
            case "comun": default: return 1;
        }
    }

    private PujaMensajeDTO convertirPujaADto(Puja puja) {
        Cliente cliente = puja.getAsistente().getCliente();
        Persona persona = cliente.getPersona();
        String fechaHora = puja.getFechaHora() != null ? puja.getFechaHora().toString() : null;
        String nombre = persona != null ? persona.getNombre() : null;
        String apellido = persona != null ? persona.getApellido() : null;
        return new PujaMensajeDTO(puja.getItemCatalogo().getId(), puja.getImporte(), fechaHora, cliente.getIdentificador(), nombre, apellido);
    }

    private static class EstadoItemActivo {
        private final Integer subastaId;
        private final Integer itemId;
        private LocalDateTime deadline;
        private Double importeActual;
        private boolean cerrando;

        private EstadoItemActivo(Integer subastaId, Integer itemId, LocalDateTime deadline, Double importeActual) {
            this.subastaId = subastaId;
            this.itemId = itemId;
            this.deadline = deadline;
            this.importeActual = importeActual;
            this.cerrando = false;
        }
    }
}
