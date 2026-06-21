package com.grupo6.subastar.service;
import com.grupo6.subastar.repository.CuentaDestinoRepository;
import com.grupo6.subastar.model.CuentaDestino;
import com.grupo6.subastar.dto.ConsignacionResponse;
import com.grupo6.subastar.dto.CuentaDestinoDTO;
import com.grupo6.subastar.dto.CuentaDestinoRequest;
import com.grupo6.subastar.dto.MensajeResponse;
import com.grupo6.subastar.dto.RespuestaConsignacionRequest;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Catalogo;
import com.grupo6.subastar.model.Duenio;
import com.grupo6.subastar.model.Deposito;
import com.grupo6.subastar.model.DocumentoConsignacion;
import com.grupo6.subastar.model.Foto;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.model.Seguro;
import com.grupo6.subastar.model.SolicitudConsignacion;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.DuenioRepository;
import com.grupo6.subastar.repository.DepositoRepository;
import com.grupo6.subastar.repository.DocumentoConsignacionRepository;
import com.grupo6.subastar.repository.FotoRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import com.grupo6.subastar.repository.ProductoRepository;
import com.grupo6.subastar.repository.RegistroSubastaRepository;
import com.grupo6.subastar.repository.SeguroRepository;
import com.grupo6.subastar.repository.SolicitudConsignacionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ConsignacionService {

    private static final int EMPLEADO_SISTEMA_ID = 1;
    private static final double COMISION_EMPRESA = 15.0;
    private static final BigDecimal PORCENTAJE_DEVOLUCION = new BigDecimal("0.05");
    private static final String INSTRUCCION_DEVOLUCION =
            "El bien debe retirarse del deposito. Si no se retira, sera devuelto "
                    + "al domicilio declarado y el costo quedara a cargo del duenio.";

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private DuenioRepository duenioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private FotoRepository fotoRepository;
    @Autowired
    private SolicitudConsignacionRepository solicitudRepository;
    @Autowired
    private CuentaDestinoRepository cuentaDestinoRepository;
    @Autowired
    private RegistroSubastaRepository registroSubastaRepository;
    @Autowired
    private SeguroRepository seguroRepository;
    @Autowired
    private DepositoRepository depositoRepository;
    @Autowired
    private ItemCatalogoRepository itemCatalogoRepository;
    @Autowired
    private DocumentoConsignacionRepository documentoRepository;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private NotificacionesReactivasService notificacionesReactivasService;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ConsignacionResponse> listar(String emailUsuario) {
        Cliente cliente = obtenerCliente(emailUsuario);
        return solicitudRepository.findByClienteId(cliente.getIdentificador())
                .stream()
                .map(this::aResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsignacionResponse crear(String emailUsuario,
                                      String tipoBien,
                                      String descripcion,
                                      String artista,
                                      String fechaCreacion,
                                      String historia,
                                      Boolean declaraPropiedad,
                                      List<MultipartFile> fotos) {
        if (esBlanco(tipoBien) || esBlanco(descripcion) || declaraPropiedad == null || !declaraPropiedad) {
            throw new RuntimeException("400: Campos obligatorios faltantes o declaracion de propiedad invalida");
        }
        if (fotos == null || fotos.size() < 6) {
            throw new RuntimeException("400: Menos de 6 fotos");
        }
        for (MultipartFile foto : fotos) {
            if (foto == null || foto.isEmpty() || foto.getContentType() == null || !foto.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new RuntimeException("422: Formato de imagen invalido");
            }
        }

        Cliente cliente = obtenerCliente(emailUsuario);
        asegurarDuenio(cliente);

        Producto producto = new Producto();
        producto.setTipo(tipoBien);
        producto.setDescripcion(descripcion);
        producto.setArtista(artista);
        producto.setFechaCreacion(fechaCreacion);
        producto.setHistoria(historia);
        producto.setDuenio(cliente.getIdentificador());
        producto.setFecha(LocalDate.now());
        producto.setDisponible("no");
        producto.setRevisorId(EMPLEADO_SISTEMA_ID);
        producto = productoRepository.saveAndFlush(producto);

        for (MultipartFile archivo : fotos) {
            Foto foto = new Foto();
            foto.setProducto(producto);
            try {
                foto.setUrlFoto(cloudinaryService.subirImagen(archivo));
            } catch (IOException e) {
                throw new RuntimeException("500: Error interno del servidor");
            }
            fotoRepository.save(foto);
        }

        SolicitudConsignacion solicitud = new SolicitudConsignacion();
        solicitud.setProducto(producto);
        solicitud.setEstado("pendiente");
        solicitud.setCondicionesAceptadas("no");
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud = solicitudRepository.save(solicitud);

        notificacionesReactivasService.notificarConsignacionEnviada(
            cliente,
            solicitud.getProducto().getDescripcion(),
            solicitud.getIdentificador()
        );
        return aResponse(solicitud);
    }

    @Transactional(readOnly = true)
    public ConsignacionResponse obtenerDetalle(String emailUsuario, Integer id) {
        Cliente cliente = obtenerCliente(emailUsuario);
        SolicitudConsignacion solicitud = solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));
        return aResponse(solicitud);
    }

    @Transactional
    public MensajeResponse responderCondiciones(String emailUsuario, Integer id, RespuestaConsignacionRequest request) {
        Cliente cliente = obtenerCliente(emailUsuario);
        SolicitudConsignacion solicitud = solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));

        if (!"aceptado".equals(normalizar(solicitud.getEstado()))) {
            throw new RuntimeException("400: La consignacion no tiene condiciones pendientes");
        }
        if ("si".equals(normalizar(solicitud.getCondicionesAceptadas()))) {
            throw new RuntimeException("409: Ya fue respondida anteriormente");
        }
        if (request == null || request.getAcepta() == null) {
            throw new RuntimeException("400: Solicitud invalida");
        }

        // CASO 1: EL CLIENTE ACEPTA LAS CONDICIONES
        if (request.getAcepta()) {
            crearItemConfirmado(solicitud);
            solicitud.setCondicionesAceptadas("si");
            solicitud.getProducto().setDisponible("si");
            productoRepository.save(solicitud.getProducto());
            solicitudRepository.save(solicitud);

            //Disparo de notificación de Éxito
            notificacionesReactivasService.notificarAceptacionOfertaCliente(
                    cliente,
                    solicitud.getProducto().getDescripcion(),
                    describirSubasta(null, buscarCatalogoPropuesto(solicitud)),
                    solicitud.getIdentificador()
            );

            return new MensajeResponse("Respuesta registrada correctamente");
        }

        // CASO 2: EL CLIENTE RECHAZA LAS CONDICIONES
        registrarCostoDevolucion(solicitud);
        solicitud.setEstado("rechazado");
        solicitud.setMotivoRechazo("Condiciones rechazadas por el cliente");
        solicitud.setCondicionesAceptadas("no");
        solicitud.getProducto().setDisponible("no");
        productoRepository.save(solicitud.getProducto());
        solicitudRepository.save(solicitud);

        // NUEVO: Disparo de notificación de Devolución
        // La consigna exige informar sucursal de retiro y cargo por devolución
        Deposito deposito = buscarDeposito(solicitud.getProducto());
        String sucursalRetiro = deposito == null
                ? "la sucursal indicada por la casa de subastas"
                : deposito.getNombre() + " - " + deposito.getDireccion();
        double cargoDevolucion = solicitud.getCostoDevolucion() == null
                ? 0.0 : solicitud.getCostoDevolucion().doubleValue();

        notificacionesReactivasService.notificarOfertaRechazadaPorCliente(
                cliente,
                solicitud.getProducto().getDescripcion(), // Obtiene el nombre del bien
                sucursalRetiro,
                cargoDevolucion,
                solicitud.getMonedaDevolucion(),
                solicitud.getIdentificador()
        );

        return new MensajeResponse("Respuesta registrada correctamente");
    }

    @Transactional
public MensajeResponse registrarCuentaDestino(String emailUsuario, Integer id, CuentaDestinoRequest request) {
    Cliente cliente = obtenerCliente(emailUsuario);

    SolicitudConsignacion solicitud = solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
            .orElseThrow(() -> new RuntimeException("403: La consignacion no pertenece al cliente"));

    if (request == null || esBlanco(request.getBanco()) || esBlanco(request.getCbu_iban()) ||
            esBlanco(request.getPais()) || esBlanco(request.getMoneda())) {
        throw new RuntimeException("400: Solicitud invalida");
    }

    String moneda = request.getMoneda().trim().toUpperCase(Locale.ROOT);
    if (!"ARS".equals(moneda) && !"USD".equals(moneda)) {
        throw new RuntimeException("400: Solicitud invalida");
    }

    if (cuentaDestinoRepository.existsBySolicitudIdentificadorAndActiva(id, "si")) {
        throw new RuntimeException("409: La cuenta destino ya fue registrada");
    }

    ItemCatalogo item = buscarItemCatalogo(solicitud.getProducto());
    if (item != null && subastaYaInicio(item)) {
        throw new RuntimeException("409: La subasta ya inicio, no se puede modificar la cuenta");
    }

    asegurarDuenio(cliente);

    Duenio duenio = duenioRepository.findById(cliente.getIdentificador())
            .orElseThrow(() -> new RuntimeException("500: Error interno del servidor"));

    CuentaDestino cuentaDestino = new CuentaDestino();
    cuentaDestino.setDuenio(duenio);
    cuentaDestino.setSolicitud(solicitud);
    cuentaDestino.setBanco(request.getBanco().trim());
    cuentaDestino.setCbuIban(request.getCbu_iban().trim());
    cuentaDestino.setPais(request.getPais().trim());
    cuentaDestino.setMoneda(moneda);
    cuentaDestino.setActiva("si");

    cuentaDestinoRepository.save(cuentaDestino);

    return new MensajeResponse("Cuenta destino registrada");
}

private boolean subastaYaInicio(ItemCatalogo item) {
    if (item == null || item.getCatalogo() == null || item.getCatalogo().getSubasta() == null) {
        return false;
    }

    if (item.getCatalogo().getSubasta().getFecha() == null || item.getCatalogo().getSubasta().getHora() == null) {
        return false;
    }

    LocalDateTime inicioSubasta = LocalDateTime.of(
            item.getCatalogo().getSubasta().getFecha(),
            item.getCatalogo().getSubasta().getHora()
    );

    return !LocalDateTime.now().isBefore(inicioSubasta);
}

private CuentaDestino buscarCuentaDestino(SolicitudConsignacion solicitud) {
    if (solicitud == null || solicitud.getIdentificador() == null) {
        return null;
    }

    return cuentaDestinoRepository
            .findBySolicitudIdentificadorAndActiva(solicitud.getIdentificador(), "si")
            .orElse(null);
}

    public MensajeResponse registrarDocumentacion(String emailUsuario, Integer id, List<MultipartFile> archivos, String descripcion) {
        Cliente cliente = obtenerCliente(emailUsuario);
        SolicitudConsignacion solicitud = solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));
        if (archivos == null || archivos.isEmpty()) {
            throw new RuntimeException("400: Sin archivos o formato invalido");
        }
        if (!"documentacion_pendiente".equals(normalizar(solicitud.getEstado()))) {
            throw new RuntimeException("409: La documentacion no fue requerida o el caso ya fue resuelto");
        }
        for (MultipartFile archivo : archivos) {
            if (!archivoValido(archivo)) {
                throw new RuntimeException("400: Sin archivos o formato invalido");
            }
        }

        for (MultipartFile archivo : archivos) {
            DocumentoConsignacion documento = new DocumentoConsignacion();
            documento.setSolicitud(solicitud);
            documento.setNombreArchivo(nombreArchivo(archivo));
            documento.setDescripcion(descripcion);
            documento.setFechaCarga(LocalDateTime.now());
            documento.setEstado("pendiente");
            try {
                documento.setUrlArchivo(cloudinaryService.subirArchivo(archivo));
            } catch (IOException e) {
                throw new RuntimeException("500: Error interno del servidor");
            }
            documentoRepository.save(documento);
        }
        solicitud.setEstado("documentacion_presentada");
        solicitudRepository.save(solicitud);
        notificacionesReactivasService.notificarDocumentacionRecibida(
                cliente,
                solicitud.getProducto().getDescripcion(),
                solicitud.getIdentificador());
        return new MensajeResponse("Documentacion recibida correctamente");
    }

    private Cliente obtenerCliente(String emailUsuario) {
        return clienteRepository.findByPersonaEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("401: Token invalido, ausente o expirado"));
    }

    private void asegurarDuenio(Cliente cliente) {
        if (duenioRepository.existsById(cliente.getIdentificador())) return;
        Duenio duenio = new Duenio();
        duenio.setId(cliente.getIdentificador());
        duenio.setPersona(cliente.getPersona());
        duenio.setNumeroPais(cliente.getPais() != null ? cliente.getPais().getNumero() : null);
        duenio.setVerificadorId(EMPLEADO_SISTEMA_ID);
        entityManager.persist(duenio);
        entityManager.flush();
    }

    private ConsignacionResponse aResponse(SolicitudConsignacion solicitud) {
        Producto producto = solicitud.getProducto();
        Seguro seguro = buscarSeguro(producto);
        Deposito deposito = buscarDeposito(producto);
        ItemCatalogo item = buscarItemCatalogo(producto);
        Catalogo catalogoPropuesto = buscarCatalogoPropuesto(solicitud);
        CuentaDestino cuentaDestino = buscarCuentaDestino(solicitud);
        var ventaPagada = registroSubastaRepository
                .findFirstByProductoIdAndEstadoPago(producto.getId(), "pagada")
                .orElse(null);

        ConsignacionResponse response = new ConsignacionResponse();

        response.setIdentificador(solicitud.getIdentificador());
        response.setEstado(ventaPagada == null
                ? solicitud.getEstado() : "vendida");
        response.setMotivoRechazo(solicitud.getMotivoRechazo());
        response.setCostoDevolucion(solicitud.getCostoDevolucion() == null
                ? null : solicitud.getCostoDevolucion().doubleValue());
        response.setMonedaDevolucion(solicitud.getMonedaDevolucion());
        response.setInstruccionDevolucion(
                "rechazado".equals(normalizar(solicitud.getEstado()))
                        ? INSTRUCCION_DEVOLUCION : null);
        response.setMotivoDocumentacion(solicitud.getMotivoDocumentacion());
        response.setCondicionesAceptadas("si".equals(normalizar(solicitud.getCondicionesAceptadas())));
        response.setFechaSolicitud(solicitud.getFechaSolicitud());
        response.setProducto(aProductoDto(producto));
        response.setCondicionesEmpresa(
                aCondicionesDto(solicitud, seguro, item, catalogoPropuesto));
        response.setUbicacionDeposito(aUbicacionDto(deposito));
        response.setSeguro(aSeguroDto(seguro, item, catalogoPropuesto));
        response.setDocumentosOrigen(aDocumentosDto(solicitud));
        response.setInstancias(aInstanciasDto(solicitud, deposito, seguro, item));
        if (ventaPagada != null) {
            response.setImporteFinalObtenido(
                    valor(ventaPagada.getImporte())
                            - valor(ventaPagada.getComision()));
        }
        if (cuentaDestino != null) {
        response.setCuentaDestino(new CuentaDestinoDTO(
                cuentaDestino.getIdentificador(),
                cuentaDestino.getBanco(),
                cuentaDestino.getCbuIban(),
                cuentaDestino.getPais(),
                cuentaDestino.getMoneda()
        ));
}
        return response;
    }

    private ConsignacionResponse.ProductoConsignadoDTO aProductoDto(Producto producto) {
        ConsignacionResponse.ProductoConsignadoDTO dto = new ConsignacionResponse.ProductoConsignadoDTO();
        dto.setIdentificador(producto.getId());
        dto.setTipoBien(producto.getTipo());
        dto.setDescripcion(producto.getDescripcion());
        dto.setArtista(producto.getArtista());
        dto.setFechaCreacion(producto.getFechaCreacion());
        dto.setHistoria(producto.getHistoria());
        if (producto.getFotos() != null) {
            dto.setFotos(producto.getFotos().stream().map(Foto::getUrlFoto).collect(Collectors.toList()));
        }
        return dto;
    }

    private ConsignacionResponse.CondicionesEmpresaDTO aCondicionesDto(
            SolicitudConsignacion solicitud,
            Seguro seguro,
            ItemCatalogo item,
            Catalogo catalogoPropuesto) {
        String estado = normalizar(solicitud.getEstado());
        if (!"aceptado".equals(estado) && !"vendida".equals(estado)) {
            return null;
        }
        ConsignacionResponse.CondicionesEmpresaDTO dto = new ConsignacionResponse.CondicionesEmpresaDTO();

        Double precioBase = null;
        if (solicitud.getPrecioBasePropuesto() != null) {
            precioBase = solicitud.getPrecioBasePropuesto().doubleValue();
        } else if (item != null) {
            precioBase = item.getPrecioBase();
        }
        dto.setPrecioBase(precioBase);

        dto.setComisionEmpresa(COMISION_EMPRESA);
        dto.setSeguroPoliza(seguro == null ? null : seguro.getNroPoliza());
        dto.setContactoPoliza(seguro == null ? null : seguro.getCompania());
        dto.setSubastaAsignada(describirSubasta(item, catalogoPropuesto));
        dto.setMoneda(monedaSubasta(item, catalogoPropuesto));
        return dto;
    }

    private ConsignacionResponse.UbicacionDepositoDTO aUbicacionDto(Deposito deposito) {
        if (deposito == null) return null;
        ConsignacionResponse.UbicacionDepositoDTO dto = new ConsignacionResponse.UbicacionDepositoDTO();
        dto.setNombre(deposito.getNombre());
        String direccion = deposito.getDireccion();
        if (!esBlanco(deposito.getSector())) {
            direccion += " - " + deposito.getSector();
        }
        dto.setDireccion(direccion);
        return dto;
    }

    private ConsignacionResponse.SeguroDTO aSeguroDto(
            Seguro seguro,
            ItemCatalogo item,
            Catalogo catalogoPropuesto) {
        if (seguro == null) return null;
        ConsignacionResponse.SeguroDTO dto = new ConsignacionResponse.SeguroDTO();
        dto.setNroPoliza(seguro.getNroPoliza());
        dto.setCompania(seguro.getCompania());
        dto.setImporte(seguro.getImporte() == null ? null : seguro.getImporte().doubleValue());
        dto.setPolizaCombinada(seguro.getPolizaCombinada());
        dto.setMoneda(monedaSubasta(item, catalogoPropuesto));
        return dto;
    }

    private List<ConsignacionResponse.InstanciaDTO> aInstanciasDto(
            SolicitudConsignacion solicitud,
            Deposito deposito,
            Seguro seguro,
            ItemCatalogo item) {
        String fecha = solicitud.getFechaSolicitud() != null
                ? solicitud.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                : "--";
        boolean aceptado = "aceptado".equals(normalizar(solicitud.getEstado()));
        boolean rechazado = "rechazado".equals(normalizar(solicitud.getEstado()));
        boolean documentacionPendiente =
                "documentacion_pendiente".equals(normalizar(solicitud.getEstado()));
        boolean documentacionPresentada =
                "documentacion_presentada".equals(normalizar(solicitud.getEstado()));
        boolean inspeccionPendiente =
                "pendiente".equals(normalizar(solicitud.getEstado()));
        boolean condiciones = "si".equals(normalizar(solicitud.getCondicionesAceptadas()));
        boolean vendida = registroSubastaRepository
                .findFirstByProductoIdAndEstadoPago(
                        solicitud.getProducto().getId(), "pagada")
                .isPresent();
        boolean recibido = deposito != null;
        boolean inspeccionado = aceptado || rechazado;
        boolean condicionesDisponibles = aceptado && seguro != null
        && ((solicitud.getCatalogoPropuestoId() != null && solicitud.getPrecioBasePropuesto() != null)
             || item != null);
        boolean asignado = condiciones && item != null;

        List<ConsignacionResponse.InstanciaDTO> instancias = new ArrayList<>();
        instancias.add(new ConsignacionResponse.InstanciaDTO("Solicitud enviada", fecha, true, false));
        instancias.add(new ConsignacionResponse.InstanciaDTO(
                "Recibido en deposito", recibido ? fecha : "--", recibido,
                recibido && inspeccionPendiente));
        if (rechazado) {
            instancias.add(new ConsignacionResponse.InstanciaDTO(
                    "Consignacion rechazada", fecha, true, true));
            return instancias;
        }
        if (documentacionPendiente || documentacionPresentada) {
            instancias.add(new ConsignacionResponse.InstanciaDTO(
                    "Documentacion de origen",
                    documentacionPresentada ? fecha : "--",
                    documentacionPresentada,
                    documentacionPendiente || documentacionPresentada));
        }
        instancias.add(new ConsignacionResponse.InstanciaDTO(
                "Inspeccionado y aceptado", aceptado ? fecha : "--", aceptado,
                inspeccionado && !rechazado && !condicionesDisponibles));
        instancias.add(new ConsignacionResponse.InstanciaDTO(
                "Esperando aceptacion de condiciones", condiciones ? fecha : "--", condiciones,
                condicionesDisponibles && !condiciones));
        instancias.add(new ConsignacionResponse.InstanciaDTO(
                "Asignacion a subasta", asignado ? fecha : "--", asignado,
                condiciones && !asignado));
        if (vendida) {
            instancias.add(new ConsignacionResponse.InstanciaDTO(
                    "Bien vendido y pagado", fecha, true, false));
            instancias.add(new ConsignacionResponse.InstanciaDTO(
                    "Transferencia enviada al propietario",
                    fecha, true, false));
        }
        return instancias;
    }

    private List<ConsignacionResponse.DocumentoDTO> aDocumentosDto(
            SolicitudConsignacion solicitud) {
        return documentoRepository
                .findBySolicitudIdentificadorOrderByFechaCargaAsc(
                        solicitud.getIdentificador())
                .stream()
                .map(documento -> {
                    ConsignacionResponse.DocumentoDTO dto =
                            new ConsignacionResponse.DocumentoDTO();
                    dto.setIdentificador(documento.getIdentificador());
                    dto.setNombreArchivo(documento.getNombreArchivo());
                    dto.setUrlArchivo(documento.getUrlArchivo());
                    dto.setDescripcion(documento.getDescripcion());
                    dto.setFechaCarga(documento.getFechaCarga());
                    dto.setEstado(documento.getEstado());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private boolean archivoValido(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return false;
        String tipo = archivo.getContentType();
        if (tipo == null) return false;
        String normalizado = tipo.toLowerCase(Locale.ROOT);
        return normalizado.startsWith("image/")
                || "application/pdf".equals(normalizado);
    }

    private String nombreArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) return "documento";
        return nombre.length() <= 250 ? nombre : nombre.substring(nombre.length() - 250);
    }

    private Seguro buscarSeguro(Producto producto) {
        if (producto == null || esBlanco(producto.getSeguro())) return null;
        return seguroRepository.findById(producto.getSeguro()).orElse(null);
    }

    private Deposito buscarDeposito(Producto producto) {
        if (producto == null || producto.getDepositoActual() == null) return null;
        return depositoRepository.findById(producto.getDepositoActual()).orElse(null);
    }

    private ItemCatalogo buscarItemCatalogo(Producto producto) {
        if (producto == null || producto.getId() == null) return null;
        List<ItemCatalogo> items = itemCatalogoRepository.findByProductoIdOrderByIdDesc(producto.getId());
        return items.isEmpty() ? null : items.get(0);
    }

    private String describirSubasta(
            ItemCatalogo item,
            Catalogo catalogoPropuesto) {
        Catalogo catalogo = item != null ? item.getCatalogo() : catalogoPropuesto;
        if (catalogo == null || catalogo.getSubasta() == null) {
            return null;
        }
        var subasta = catalogo.getSubasta();
        String fecha = subasta.getFecha() == null ? "" : subasta.getFecha().toString();
        String hora = subasta.getHora() == null ? "" : subasta.getHora().toString();
        return (fecha + " " + hora).trim();
    }

    private String monedaSubasta(
            ItemCatalogo item,
            Catalogo catalogoPropuesto) {
        Catalogo catalogo = item != null ? item.getCatalogo() : catalogoPropuesto;
        if (catalogo == null || catalogo.getSubasta() == null) {
            return null;
        }
        return catalogo.getSubasta().getMoneda();
    }

    private void registrarCostoDevolucion(SolicitudConsignacion solicitud) {
        if (solicitud.getPrecioBasePropuesto() == null) {
            solicitud.setCostoDevolucion(null);
            solicitud.setMonedaDevolucion(null);
            return;
        }
        solicitud.setCostoDevolucion(
                solicitud.getPrecioBasePropuesto().multiply(PORCENTAJE_DEVOLUCION)
                        .setScale(2, RoundingMode.HALF_UP));
        solicitud.setMonedaDevolucion(
                monedaSubasta(null, buscarCatalogoPropuesto(solicitud)));
    }

    private Catalogo buscarCatalogoPropuesto(SolicitudConsignacion solicitud) {
        if (solicitud.getCatalogoPropuestoId() == null) return null;
        return entityManager.find(Catalogo.class, solicitud.getCatalogoPropuestoId());
    }

    private void crearItemConfirmado(SolicitudConsignacion solicitud) {
        if (buscarItemCatalogo(solicitud.getProducto()) != null) return;
        Catalogo catalogo = buscarCatalogoPropuesto(solicitud);
        if (catalogo == null || solicitud.getPrecioBasePropuesto() == null) {
            throw new RuntimeException("409: Las condiciones propuestas estan incompletas");
        }
        ItemCatalogo item = new ItemCatalogo();
        item.setCatalogo(catalogo);
        item.setProducto(solicitud.getProducto());
        item.setPrecioBase(solicitud.getPrecioBasePropuesto().doubleValue());
        item.setComision(COMISION_EMPRESA);
        item.setSubastado("no");
        item.setPrecioFinal(null);
        itemCatalogoRepository.save(item);
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
