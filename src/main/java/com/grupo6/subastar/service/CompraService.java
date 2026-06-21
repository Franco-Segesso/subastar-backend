package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.CompraDTO;
import com.grupo6.subastar.dto.ModalidadEntregaResponse;
import com.grupo6.subastar.dto.PagoCompraResponse;
import com.grupo6.subastar.model.Catalogo;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.CuentaDestino;
import com.grupo6.subastar.model.Duenio;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.model.RegistroSubasta;
import com.grupo6.subastar.model.SolicitudConsignacion;
import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.CuentaDestinoRepository;
import com.grupo6.subastar.repository.DuenioRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import com.grupo6.subastar.repository.ProductoRepository;
import com.grupo6.subastar.repository.RegistroSubastaRepository;
import com.grupo6.subastar.repository.SolicitudConsignacionRepository;
import com.grupo6.subastar.repository.SubastaRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {

    private static final double COSTO_ENVIO_FIJO = 5000.0;

    private final ClienteRepository clienteRepository;
    private final RegistroSubastaRepository registroRepository;
    private final SubastaRepository subastaRepository;
    private final ProductoRepository productoRepository;
    private final ItemCatalogoRepository itemRepository;
    private final DuenioRepository duenioRepository;
    private final SolicitudConsignacionRepository solicitudRepository;
    private final CuentaDestinoRepository cuentaDestinoRepository;
    private final MedioPagoService medioPagoService;
    private final NotificacionesReactivasService notificacionesService;

    public CompraService(
            ClienteRepository clienteRepository,
            RegistroSubastaRepository registroRepository,
            SubastaRepository subastaRepository,
            ProductoRepository productoRepository,
            ItemCatalogoRepository itemRepository,
            DuenioRepository duenioRepository,
            SolicitudConsignacionRepository solicitudRepository,
            CuentaDestinoRepository cuentaDestinoRepository,
            MedioPagoService medioPagoService,
            NotificacionesReactivasService notificacionesService) {
        this.clienteRepository = clienteRepository;
        this.registroRepository = registroRepository;
        this.subastaRepository = subastaRepository;
        this.productoRepository = productoRepository;
        this.itemRepository = itemRepository;
        this.duenioRepository = duenioRepository;
        this.solicitudRepository = solicitudRepository;
        this.cuentaDestinoRepository = cuentaDestinoRepository;
        this.medioPagoService = medioPagoService;
        this.notificacionesService = notificacionesService;
    }

    @Transactional(readOnly = true)
    public CompraDTO obtenerCompra(String email, Integer compraId) {
        Cliente cliente = obtenerCliente(email);
        RegistroSubasta compra = obtenerCompraPropia(compraId, cliente);
        Subasta subasta = obtenerSubasta(compra);
        Producto producto = obtenerProducto(compra);
        ItemCatalogo item = itemRepository.findBySubastaIdAndProductoId(
                        compra.getSubastaId(), compra.getProductoId())
                .orElseThrow(() -> new RuntimeException("404: Item inexistente"));

        double costoEnvio = valor(compra.getCostoEnvio());
        double total = valor(compra.getImporte()) + valor(compra.getComision())
                + costoEnvio;
        String modalidad = normalizarModalidad(compra.getModalidadEntrega());
        String direccion = "envio".equals(modalidad)
                && cliente.getPersona() != null
                ? cliente.getPersona().getDireccion() : null;

        return new CompraDTO(
                compra.getIdentificador(),
                new CompraDTO.SubastaDTO(
                        subasta.getId(), nombreSubasta(subasta),
                        subasta.getMoneda()),
                new CompraDTO.ItemDTO(
                        item.getId(), item.getId(), producto.getDescripcion()),
                compra.getImporte(),
                compra.getComision(),
                compra.getCostoEnvio(),
                total,
                modalidad,
                direccion,
                avisoSeguro(modalidad, compra.getNroPolizaSeguro()),
                normalizarEstadoPago(compra.getEstadoPago()),
                compra.getMedioPagoId());
    }

    @Transactional
    public ModalidadEntregaResponse definirEntrega(
            String email,
            Integer compraId,
            String modalidadSolicitada) {
        Cliente cliente = obtenerCliente(email);
        RegistroSubasta compra = obtenerCompraPropia(compraId, cliente);
        String modalidad = normalizarModalidad(modalidadSolicitada);

        if (!"envio".equals(modalidad) && !"retiro".equals(modalidad)) {
            throw new RuntimeException("400: Modalidad invalida");
        }
        if ("pagada".equals(normalizarEstadoPago(compra.getEstadoPago()))) {
            throw new RuntimeException(
                    "409: No se puede modificar la entrega de una compra pagada");
        }

        compra.setModalidadEntrega(modalidad);
        compra.setCostoEnvio("envio".equals(modalidad)
                ? COSTO_ENVIO_FIJO : 0.0);
        compra.setEstadoEntrega("pendiente");
        registroRepository.save(compra);
        return new ModalidadEntregaResponse(
                "Modalidad registrada correctamente.", modalidad);
    }

    @Transactional
    public PagoCompraResponse pagar(
            String email,
            Integer compraId,
            Integer medioPagoId) {
        Cliente comprador = obtenerCliente(email);
        RegistroSubasta compra = obtenerCompraPropia(compraId, comprador);

        if ("pagada".equals(normalizarEstadoPago(compra.getEstadoPago()))) {
            throw new RuntimeException("409: La compra ya fue pagada");
        }
        if ("pendiente".equals(
                normalizarModalidad(compra.getModalidadEntrega()))) {
            throw new RuntimeException(
                    "409: Debe seleccionar la modalidad de entrega");
        }

        Subasta subasta = obtenerSubasta(compra);
        Producto producto = obtenerProducto(compra);
        SolicitudConsignacion solicitud = solicitudRepository
                .findByProductoId(producto.getId())
                .orElse(null);
        CuentaDestino cuentaDestino = solicitud == null ? null
                : cuentaDestinoRepository
                        .findBySolicitudIdentificadorAndActiva(
                                solicitud.getIdentificador(), "si")
                        .orElse(null);

        if (solicitud != null && cuentaDestino == null) {
            throw new RuntimeException(
                    "409: El vendedor no registro una cuenta destino");
        }
        if (cuentaDestino != null
                && !subasta.getMoneda().equalsIgnoreCase(
                        cuentaDestino.getMoneda())) {
            throw new RuntimeException(
                    "409: La cuenta destino no coincide con la moneda de la subasta");
        }

        double total = valor(compra.getImporte())
                + valor(compra.getComision())
                + valor(compra.getCostoEnvio());
        medioPagoService.cobrar(
                medioPagoId, comprador, subasta.getMoneda(), total);

        LocalDateTime ahora = LocalDateTime.now();
        compra.setMedioPagoId(medioPagoId);
        compra.setEstadoPago("pagada");
        compra.setFechaPago(ahora);
        compra.setEstadoEntrega("pendiente");

        transferirPropiedad(producto, comprador);
        registroRepository.save(compra);

        if (solicitud != null) {
            solicitud.setEstado("vendida");
            solicitudRepository.save(solicitud);
            double importeNeto = valor(compra.getImporte())
                    - valor(compra.getComision());
            notificacionesService.notificarTransferenciaEnviada(
                    compra.getDuenioId(),
                    producto.getDescripcion(),
                    importeNeto,
                    cuentaDestino.getCbuIban(),
                    solicitud.getIdentificador());
        }

        return new PagoCompraResponse(
                "La compra se realizo con exito.",
                compra.getIdentificador(),
                compra.getEstadoPago(),
                medioPagoId);
    }

    private void transferirPropiedad(
            Producto producto,
            Cliente comprador) {
        Duenio nuevoDuenio = duenioRepository
                .findById(comprador.getIdentificador())
                .orElseGet(() -> crearDuenio(comprador));
        producto.setDuenio(nuevoDuenio.getId());
        producto.setDisponible("no");
        productoRepository.save(producto);
    }

    private Duenio crearDuenio(Cliente cliente) {
        Duenio duenio = new Duenio();
        duenio.setId(cliente.getIdentificador());
        duenio.setPersona(cliente.getPersona());
        duenio.setNumeroPais(cliente.getPais() == null
                ? null : cliente.getPais().getNumero());
        duenio.setCalificacionRiesgo(1);
        duenio.setVerificadorId(cliente.getVerificadorId());
        return duenioRepository.save(duenio);
    }

    private RegistroSubasta obtenerCompraPropia(
            Integer compraId,
            Cliente cliente) {
        RegistroSubasta compra = registroRepository.findById(compraId)
                .orElseThrow(() -> new RuntimeException(
                        "404: Compra inexistente"));
        if (!cliente.getIdentificador().equals(compra.getClienteId())) {
            throw new RuntimeException(
                    "403: La compra no pertenece al cliente");
        }
        return compra;
    }

    private Subasta obtenerSubasta(RegistroSubasta compra) {
        return subastaRepository.findById(compra.getSubastaId())
                .orElseThrow(() -> new RuntimeException(
                        "404: Subasta inexistente"));
    }

    private Producto obtenerProducto(RegistroSubasta compra) {
        return productoRepository.findById(compra.getProductoId())
                .orElseThrow(() -> new RuntimeException(
                        "404: Producto inexistente"));
    }

    private Cliente obtenerCliente(String email) {
        return clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "401: Token invalido, ausente o expirado"));
    }

    private String nombreSubasta(Subasta subasta) {
        Catalogo catalogo = subasta.getCatalogo();
        if (catalogo == null || catalogo.getDescription() == null
                || catalogo.getDescription().isBlank()) {
            return "Subasta #" + subasta.getId();
        }
        return catalogo.getDescription();
    }

    private String avisoSeguro(String modalidad, String poliza) {
        String referencia = poliza == null || poliza.isBlank()
                ? "" : " por la poliza " + poliza;
        if ("retiro".equals(modalidad)) {
            return "Cobertura vigente" + referencia
                    + " hasta que la empresa entregue el bien.";
        }
        if ("envio".equals(modalidad)) {
            return "Cobertura vigente" + referencia
                    + " durante el traslado y hasta la entrega.";
        }
        return "El bien permanece cubierto" + referencia
                + " mientras esta bajo custodia de la empresa.";
    }

    private String normalizarModalidad(String modalidad) {
        return modalidad == null || modalidad.isBlank()
                ? "pendiente" : modalidad.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarEstadoPago(String estado) {
        return estado == null || estado.isBlank()
                ? "pendiente" : estado.trim().toLowerCase(Locale.ROOT);
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
