package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.MultaDTO;
import com.grupo6.subastar.dto.PagarMultaResponse;
import com.grupo6.subastar.model.Catalogo;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Multa;
import com.grupo6.subastar.model.Puja;
import com.grupo6.subastar.model.RegistroSubasta;
import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.model.TipoNotificacion;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.MultaRepository;
import com.grupo6.subastar.repository.PujaRepository;
import com.grupo6.subastar.repository.RegistroSubastaRepository;
import com.grupo6.subastar.repository.SubastaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MultaService {

    private static final long HORAS_PARA_PAGAR_COMPRA = 24;
    private static final long HORAS_PARA_PRESENTAR_FONDOS = 72;
    private static final int PRIMERA_PUJA_MODULO_MULTAS = 15;

    private final MultaRepository multaRepository;
    private final ClienteRepository clienteRepository;
    private final PujaRepository pujaRepository;
    private final RegistroSubastaRepository registroRepository;
    private final SubastaRepository subastaRepository;
    private final MedioPagoService medioPagoService;
    private final NotificacionService notificacionService;
    private final FirebasePushService firebasePushService;

    public MultaService(
            MultaRepository multaRepository,
            ClienteRepository clienteRepository,
            PujaRepository pujaRepository,
            RegistroSubastaRepository registroRepository,
            SubastaRepository subastaRepository,
            MedioPagoService medioPagoService,
            NotificacionService notificacionService,
            FirebasePushService firebasePushService) {
        this.multaRepository = multaRepository;
        this.clienteRepository = clienteRepository;
        this.pujaRepository = pujaRepository;
        this.registroRepository = registroRepository;
        this.subastaRepository = subastaRepository;
        this.medioPagoService = medioPagoService;
        this.notificacionService = notificacionService;
        this.firebasePushService = firebasePushService;
    }

    @Transactional(readOnly = true)
    public List<MultaDTO> listar(String email) {
        Cliente cliente = obtenerCliente(email);
        return multaRepository
                .findByClienteIdentificadorOrderByFechaGeneracionDesc(cliente.getIdentificador())
                .stream()
                .map(this::aDTO)
                .toList();
    }

    @Transactional
    public PagarMultaResponse pagar(String email, Integer multaId, Integer medioPagoId) {
        Cliente cliente = obtenerCliente(email);
        Multa multa = multaRepository
                .findByIdentificadorAndClienteIdentificador(multaId, cliente.getIdentificador())
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));

        String estado = normalizar(multa.getEstado());
        if ("pagada".equals(estado) || "judicial".equals(estado)) {
            throw new RuntimeException("409: La multa ya fue pagada o derivada a la justicia");
        }

        Subasta subasta = subastaDe(multa.getPuja());
        try {
            medioPagoService.cobrar(
                    medioPagoId,
                    cliente,
                    subasta.getMoneda(),
                    multa.getImporte().doubleValue());
        } catch (RuntimeException e) {
            String mensaje = e.getMessage() == null ? "" : e.getMessage();
            if (mensaje.startsWith("403")) {
                throw new RuntimeException("400: Medio de pago invalido o sin fondos");
            }
            throw e;
        }

        multa.setEstado("pagada");
        multa.setFechaPago(LocalDateTime.now());
        multaRepository.save(multa);

        return new PagarMultaResponse(
                "Multa pagada correctamente.",
                cliente.getCategoria());
    }

    @Transactional
    public synchronized void generarMultasVencidas(LocalDateTime ahora) {
        LocalDateTime limitePago = ahora.minusHours(HORAS_PARA_PAGAR_COMPRA);
        for (Puja puja : pujaRepository
                .findByGanadorIgnoreCaseAndFechaHoraLessThanEqual("si", limitePago)) {
            if (puja.getId() == null
                    || puja.getId() < PRIMERA_PUJA_MODULO_MULTAS) {
                continue;
            }
            if (multaRepository.existsByPujaId(puja.getId())) continue;

            Optional<RegistroSubasta> compraOpt = buscarCompra(puja);
            if (compraOpt.isEmpty()) continue;
            RegistroSubasta compra = compraOpt.get();
            if (!"pendiente".equals(normalizar(compra.getEstadoPago()))) continue;

            Cliente cliente = puja.getAsistente().getCliente();
            Multa multa = new Multa();
            multa.setCliente(cliente);
            multa.setPuja(puja);
            multa.setImporte(BigDecimal.valueOf(puja.getImporte())
                    .multiply(BigDecimal.valueOf(0.10))
                    .setScale(2, RoundingMode.HALF_UP));
            multa.setEstado("pendiente");
            LocalDateTime fechaGeneracion =
                    puja.getFechaHora().plusHours(HORAS_PARA_PAGAR_COMPRA);
            multa.setFechaGeneracion(fechaGeneracion);
            multa.setFechaVencimiento(
                    fechaGeneracion.plusHours(HORAS_PARA_PRESENTAR_FONDOS));
            multa = multaRepository.save(multa);

            notificacionService.crearNotificacion(
                    cliente,
                    "Multa pendiente",
                    "Vencio el plazo de pago del item #" + puja.getItemCatalogo().getId()
                            + ". Tenes 72 horas para presentar los fondos.",
                    TipoNotificacion.MULTA,
                    multa.getIdentificador());
            
            firebasePushService.enviarNotificacionPush(
                    cliente.getIdentificador(),
                    "Multa por falta de pago",
                    "Venció el plazo de 24hs para el ítem #" + puja.getItemCatalogo().getId() + ". Se te ha aplicado una multa del 10%.",
                    TipoNotificacion.MULTA.name(),
                    multa.getIdentificador()
            );
        }
    }

    @Transactional
    public void derivarVencidasALaJusticia(LocalDateTime ahora) {
        for (Multa multa : multaRepository
                .findByFechaVencimientoLessThanEqual(ahora)) {
            if ("judicial".equals(normalizar(multa.getEstado()))) continue;
            Optional<RegistroSubasta> compraOpt = buscarCompra(multa.getPuja());
            if (compraOpt.isEmpty()) continue;
            RegistroSubasta compra = compraOpt.get();
            boolean multaPendiente =
                    !"pagada".equals(normalizar(multa.getEstado()));
            boolean compraPendiente =
                    !"pagada".equals(normalizar(compra.getEstadoPago()));
            if (!multaPendiente && !compraPendiente) continue;

            multa.setEstado("judicial");
            multaRepository.save(multa);
            if (multa.getCliente().getPersona() != null) {
                multa.getCliente().getPersona().setEstado("inactivo");
            }

            //NOTIFICACION DE BLOQUEO POR MULTA VENCIDA
            String mensajeBloqueo = "El plazo de 72hs para regularizar tu deuda ha vencido. Tu cuenta ha sido bloqueada y el caso derivado al departamento legal.";
            
            notificacionService.crearNotificacion(
                    multa.getCliente(),
                    "Cuenta Suspendida",
                    mensajeBloqueo,
                    TipoNotificacion.MULTA, // O el tipo que usen para bloqueos/alertas
                    multa.getIdentificador()
            );

            firebasePushService.enviarNotificacionPush(
                    multa.getCliente().getIdentificador(),
                    "Cuenta Suspendida",
                    "Plazo de 72hs vencido. Tu cuenta ha sido inactivada.",
                    TipoNotificacion.MULTA.name(),
                    multa.getIdentificador()
            );
            
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeParticipar(Cliente cliente) {
        if (cliente != null && multaRepository
                .existsByClienteIdentificadorAndEstadoIgnoreCase(
                        cliente.getIdentificador(),
                        "pendiente")) {
            throw new RuntimeException("403: Tenes una multa pendiente de pago");
        }
    }

    @Transactional(readOnly = true)
    public long contarPendientes(String email) {
        Cliente cliente = obtenerCliente(email);
        return multaRepository.countByClienteIdentificadorAndEstadoIgnoreCase(
                cliente.getIdentificador(),
                "pendiente");
    }

    private MultaDTO aDTO(Multa multa) {
        Puja puja = multa.getPuja();
        Subasta subasta = subastaDe(puja);
        ItemCatalogo item = puja.getItemCatalogo();
        Optional<RegistroSubasta> compraOpt = buscarCompra(puja);
        RegistroSubasta compra = compraOpt.orElse(null);
        String nombre = nombreSubasta(subasta)
                + " - item #" + (item == null ? "?" : item.getId())
                + " (" + subasta.getMoneda() + ")";
        return new MultaDTO(
                multa.getIdentificador(),
                multa.getImporte(),
                normalizar(multa.getEstado()),
                multa.getFechaGeneracion(),
                multa.getFechaVencimiento(),
                multa.getFechaPago(),
                nombre,
                puja.getImporte(),
                compra == null ? null : compra.getIdentificador(),
                compra == null ? null : normalizar(compra.getEstadoPago()),
                compra == null ? null : valor(compra.getImporte())
                        + valor(compra.getComision())
                        + valor(compra.getCostoEnvio()));
    }

    private Optional<RegistroSubasta> buscarCompra(Puja puja) {
        ItemCatalogo item = puja.getItemCatalogo();
        Cliente cliente = puja.getAsistente().getCliente();
        return registroRepository
                .findFirstBySubastaIdAndProductoIdAndClienteId(
                        item.getCatalogo().getSubasta().getId(),
                        item.getProducto().getId(),
                        cliente.getIdentificador());
    }

    private Subasta subastaDe(Puja puja) {
        if (puja == null || puja.getItemCatalogo() == null
                || puja.getItemCatalogo().getCatalogo() == null
                || puja.getItemCatalogo().getCatalogo().getSubasta() == null) {
            throw new RuntimeException("404: Subasta inexistente");
        }
        Integer subastaId = puja.getItemCatalogo().getCatalogo().getSubasta().getId();
        return subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RuntimeException("404: Subasta inexistente"));
    }

    private String nombreSubasta(Subasta subasta) {
        Catalogo catalogo = subasta.getCatalogo();
        if (catalogo == null || catalogo.getDescription() == null
                || catalogo.getDescription().isBlank()) {
            return "Subasta #" + subasta.getId();
        }
        return catalogo.getDescription();
    }

    private Cliente obtenerCliente(String email) {
        return clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException("401: Token invalido, ausente o expirado"));
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
