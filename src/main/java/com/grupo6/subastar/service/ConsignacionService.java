package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.ConsignacionResponse;
import com.grupo6.subastar.dto.CuentaDestinoRequest;
import com.grupo6.subastar.dto.MensajeResponse;
import com.grupo6.subastar.dto.RespuestaConsignacionRequest;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.CuentaBancaria;
import com.grupo6.subastar.model.Duenio;
import com.grupo6.subastar.model.Foto;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.model.SolicitudConsignacion;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.CuentaBancariaRepository;
import com.grupo6.subastar.repository.DuenioRepository;
import com.grupo6.subastar.repository.FotoRepository;
import com.grupo6.subastar.repository.ProductoRepository;
import com.grupo6.subastar.repository.SolicitudConsignacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private CuentaBancariaRepository cuentaBancariaRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

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

        if (request.getAcepta()) {
            solicitud.setCondicionesAceptadas("si");
            solicitudRepository.save(solicitud);
            return new MensajeResponse("Respuesta registrada correctamente");
        }

        solicitud.setEstado("rechazado");
        solicitud.setMotivoRechazo("Condiciones rechazadas por el cliente");
        solicitud.setCondicionesAceptadas("no");
        solicitudRepository.save(solicitud);
        return new MensajeResponse("Respuesta registrada correctamente");
    }

    @Transactional
    public MensajeResponse registrarCuentaDestino(String emailUsuario, Integer id, CuentaDestinoRequest request) {
        Cliente cliente = obtenerCliente(emailUsuario);
        solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));
        if (request == null || esBlanco(request.getBanco()) || esBlanco(request.getCbu_iban()) ||
                esBlanco(request.getPais()) || esBlanco(request.getMoneda())) {
            throw new RuntimeException("400: Solicitud invalida");
        }
        String moneda = request.getMoneda().toUpperCase(Locale.ROOT);
        if (!"ARS".equals(moneda) && !"USD".equals(moneda)) {
            throw new RuntimeException("400: Solicitud invalida");
        }

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setCliente(cliente);
        cuenta.setActivo("si");
        cuenta.setCbuIban(request.getCbu_iban());
        cuenta.setBanco(request.getBanco());
        cuenta.setPaisBanco(request.getPais());
        cuenta.setMoneda(moneda);
        cuentaBancariaRepository.save(cuenta);
        return new MensajeResponse("Cuenta destino registrada");
    }

    public MensajeResponse registrarDocumentacion(String emailUsuario, Integer id, List<MultipartFile> archivos, String descripcion) {
        Cliente cliente = obtenerCliente(emailUsuario);
        solicitudRepository.findByIdAndClienteId(id, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));
        if (archivos == null || archivos.isEmpty()) {
            throw new RuntimeException("400: Sin archivos o formato invalido");
        }
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
        duenioRepository.save(duenio);
    }

    private ConsignacionResponse aResponse(SolicitudConsignacion solicitud) {
        Producto producto = solicitud.getProducto();

        ConsignacionResponse response = new ConsignacionResponse();
        response.setIdentificador(solicitud.getIdentificador());
        response.setEstado(solicitud.getEstado());
        response.setMotivoRechazo(solicitud.getMotivoRechazo());
        response.setCondicionesAceptadas("si".equals(normalizar(solicitud.getCondicionesAceptadas())));
        response.setFechaSolicitud(solicitud.getFechaSolicitud());
        response.setProducto(aProductoDto(producto));
        response.setCondicionesEmpresa(aCondicionesDto(solicitud));
        response.setUbicacionDeposito(aUbicacionDto());
        response.setSeguro(aSeguroDto());
        response.setInstancias(aInstanciasDto(solicitud));
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

    private ConsignacionResponse.CondicionesEmpresaDTO aCondicionesDto(SolicitudConsignacion solicitud) {
        if (!"aceptado".equals(normalizar(solicitud.getEstado()))) return null;
        ConsignacionResponse.CondicionesEmpresaDTO dto = new ConsignacionResponse.CondicionesEmpresaDTO();
        dto.setPrecioBase(3000.0);
        dto.setComisionEmpresa(10.0);
        dto.setSeguroPoliza("USD 60/mes");
        dto.setContactoPoliza("+54 9 11 2380-1603");
        dto.setSubastaAsignada("15 Abr 2026");
        return dto;
    }

    private ConsignacionResponse.UbicacionDepositoDTO aUbicacionDto() {
        ConsignacionResponse.UbicacionDepositoDTO dto = new ConsignacionResponse.UbicacionDepositoDTO();
        dto.setNombre("Deposito Central Norte");
        dto.setDireccion("Av. Del Libertador 8500, CABA - Sector B - Estante 14");
        return dto;
    }

    private ConsignacionResponse.SeguroDTO aSeguroDto() {
        ConsignacionResponse.SeguroDTO dto = new ConsignacionResponse.SeguroDTO();
        dto.setNroPoliza("POL-DAI-2026");
        dto.setCompania("Subastar Seguros");
        dto.setImporte(60.0);
        return dto;
    }

    private List<ConsignacionResponse.InstanciaDTO> aInstanciasDto(SolicitudConsignacion solicitud) {
        String fecha = solicitud.getFechaSolicitud() != null
                ? solicitud.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                : "--";
        boolean aceptado = "aceptado".equals(normalizar(solicitud.getEstado()));
        boolean rechazado = "rechazado".equals(normalizar(solicitud.getEstado()));
        boolean condiciones = "si".equals(normalizar(solicitud.getCondicionesAceptadas()));

        List<ConsignacionResponse.InstanciaDTO> instancias = new ArrayList<>();
        instancias.add(new ConsignacionResponse.InstanciaDTO("Solicitud enviada", fecha, true, false));
        instancias.add(new ConsignacionResponse.InstanciaDTO("Recibido en deposito", aceptado || rechazado ? fecha : "--", aceptado || rechazado, false));
        instancias.add(new ConsignacionResponse.InstanciaDTO("Inspeccionado y aceptado", aceptado ? fecha : "--", aceptado, aceptado && !condiciones));
        instancias.add(new ConsignacionResponse.InstanciaDTO("Esperando aceptacion de condiciones", aceptado && condiciones ? fecha : "--", aceptado && condiciones, aceptado && !condiciones));
        instancias.add(new ConsignacionResponse.InstanciaDTO("Asignacion a subasta", condiciones ? fecha : "--", condiciones, condiciones));
        return instancias;
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
