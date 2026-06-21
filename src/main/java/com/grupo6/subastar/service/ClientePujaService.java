package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.HistorialPujasClienteDTO;
import com.grupo6.subastar.dto.MetricasClienteDTO;
import com.grupo6.subastar.dto.SubastaParticipacionDTO;
import com.grupo6.subastar.model.Catalogo;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.model.Puja;
import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.PujaRepository;
import com.grupo6.subastar.repository.RegistroSubastaRepository;
import com.grupo6.subastar.repository.SolicitudConsignacionRepository;
import com.grupo6.subastar.repository.SubastaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClientePujaService {

    private final ClienteRepository clienteRepository;
    private final PujaRepository pujaRepository;
    private final SubastaRepository subastaRepository;
    private final SolicitudConsignacionRepository consignacionRepository;
    private final RegistroSubastaRepository registroSubastaRepository;

    public ClientePujaService(
            ClienteRepository clienteRepository,
            PujaRepository pujaRepository,
            SubastaRepository subastaRepository,
            SolicitudConsignacionRepository consignacionRepository,
            RegistroSubastaRepository registroSubastaRepository) {
        this.clienteRepository = clienteRepository;
        this.pujaRepository = pujaRepository;
        this.subastaRepository = subastaRepository;
        this.consignacionRepository = consignacionRepository;
        this.registroSubastaRepository = registroSubastaRepository;
    }

    @Transactional(readOnly = true)
    public List<SubastaParticipacionDTO> listarSubastas(String email, String resultado) {
        Cliente cliente = obtenerCliente(email);
        Map<Integer, List<Puja>> porSubasta = agruparPorSubasta(
                pujaRepository.findHistorialByClienteId(cliente.getIdentificador()));

        return porSubasta.values().stream()
                .map(this::crearParticipacion)
                .filter(item -> coincideResultado(item, resultado))
                .toList();
    }

    @Transactional(readOnly = true)
    public HistorialPujasClienteDTO obtenerHistorial(String email, Integer subastaId) {
        Cliente cliente = obtenerCliente(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RuntimeException("404: Recurso no encontrado"));

        List<Puja> todasLasPujas = pujaRepository.findHistorialBySubastaId(subastaId);
        List<Puja> pujasCliente = todasLasPujas.stream()
                .filter(p -> cliente.getIdentificador().equals(
                        p.getAsistente().getCliente().getIdentificador()))
                .toList();

        if (pujasCliente.isEmpty()) {
            throw new RuntimeException("403: El cliente no participo en esa subasta");
        }

        double totalPujado = pujasCliente.stream()
                .mapToDouble(p -> valor(p.getImporte()))
                .sum();
        double totalPagado = pujasCliente.stream()
                .filter(this::esGanadora)
                .mapToDouble(p -> valor(p.getImporte()))
                .sum();
        boolean gano = pujasCliente.stream().anyMatch(this::esGanadora);

        List<HistorialPujasClienteDTO.PujaDTO> historial = new ArrayList<>();
        for (int indice = 0; indice < pujasCliente.size(); indice++) {
            Puja puja = pujasCliente.get(indice);
            Optional<com.grupo6.subastar.model.RegistroSubasta> compra =
                    esGanadora(puja)
                            ? registroSubastaRepository
                            .findFirstBySubastaIdAndProductoIdAndClienteId(
                                    subastaId,
                                    puja.getItemCatalogo().getProducto().getId(),
                                    cliente.getIdentificador())
                            : Optional.empty();
            historial.add(new HistorialPujasClienteDTO.PujaDTO(
                    indice + 1,
                    puja.getItemCatalogo().getId(),
                    puja.getItemCatalogo().getProducto().getDescripcion(),
                    puja.getImporte(),
                    puja.getFechaHora(),
                    esGanadora(puja),
                    compra.map(registro -> registro.getIdentificador())
                            .orElse(null),
                    compra.map(registro -> registro.getEstadoPago())
                            .orElse(null),
                    buscarPujaSuperadora(puja, todasLasPujas)));
        }

        return new HistorialPujasClienteDTO(
                new HistorialPujasClienteDTO.SubastaDTO(
                        subasta.getId(),
                        nombreSubasta(subasta),
                        subasta.getMoneda()),
                new HistorialPujasClienteDTO.ResumenDTO(
                        totalPujado,
                        totalPagado,
                        pujasCliente.size(),
                        gano),
                historial);
    }

    @Transactional(readOnly = true)
    public MetricasClienteDTO obtenerMetricas(String email) {
        Cliente cliente = obtenerCliente(email);
        List<Puja> pujas = pujaRepository.findHistorialByClienteId(cliente.getIdentificador());
        Map<Integer, List<Puja>> porSubasta = agruparPorSubasta(pujas);

        int totalSubastas = porSubasta.size();
        int subastasGanadas = (int) porSubasta.values().stream()
                .filter(lista -> lista.stream().anyMatch(this::esGanadora))
                .count();
        int totalPujas = pujas.size();
        double totalPujado = pujas.stream().mapToDouble(p -> valor(p.getImporte())).sum();
        double totalPagado = pujas.stream()
                .filter(this::esGanadora)
                .mapToDouble(p -> valor(p.getImporte()))
                .sum();
        double pujaMaxima = pujas.stream()
                .map(Puja::getImporte)
                .filter(importe -> importe != null)
                .max(Comparator.naturalOrder())
                .orElse(0.0);
        double porcentajeExito = totalSubastas == 0
                ? 0.0
                : (subastasGanadas * 100.0) / totalSubastas;
        double promedio = totalSubastas == 0 ? 0.0 : (double) totalPujas / totalSubastas;

        Map<String, Integer> actividad = new LinkedHashMap<>();
        for (Puja puja : pujas) {
            String categoria = normalizarCategoria(
                    puja.getAsistente().getSubasta().getCategoria());
            actividad.merge(categoria, 1, Integer::sum);
        }
        List<MetricasClienteDTO.ActividadCategoriaDTO> actividadDTO = actividad.entrySet().stream()
                .map(entry -> new MetricasClienteDTO.ActividadCategoriaDTO(
                        entry.getKey(), entry.getValue()))
                .toList();

        return new MetricasClienteDTO(
                totalSubastas,
                subastasGanadas,
                porcentajeExito,
                totalPujado,
                totalPagado,
                totalPujas,
                promedio,
                pujaMaxima,
                actividadDTO,
                normalizarCategoria(cliente.getCategoria()),
                Math.toIntExact(consignacionRepository.countByClienteId(
                        cliente.getIdentificador())));
    }

    private Cliente obtenerCliente(String email) {
        return clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException("401: Token invalido, ausente o expirado"));
    }

    private Map<Integer, List<Puja>> agruparPorSubasta(List<Puja> pujas) {
        return pujas.stream().collect(Collectors.groupingBy(
                puja -> puja.getAsistente().getSubasta().getId(),
                LinkedHashMap::new,
                Collectors.toList()));
    }

    private SubastaParticipacionDTO crearParticipacion(List<Puja> pujas) {
        Subasta subasta = pujas.get(0).getAsistente().getSubasta();
        Optional<Puja> ganadora = pujas.stream().filter(this::esGanadora).findFirst();
        double mayorOferta = pujas.stream()
                .map(Puja::getImporte)
                .filter(importe -> importe != null)
                .max(Comparator.naturalOrder())
                .orElse(0.0);
        double importePagado = pujas.stream()
                .filter(this::esGanadora)
                .mapToDouble(p -> valor(p.getImporte()))
                .sum();
        Optional<com.grupo6.subastar.model.RegistroSubasta> compra = ganadora.flatMap(
                p -> registroSubastaRepository.findFirstBySubastaIdAndProductoIdAndClienteId(
                        subasta.getId(),
                        p.getItemCatalogo().getProducto().getId(),
                        p.getAsistente().getCliente().getIdentificador()));

        return new SubastaParticipacionDTO(
                new SubastaParticipacionDTO.SubastaDTO(
                        subasta.getId(),
                        nombreSubasta(subasta),
                        subasta.getFecha(),
                        subasta.getHora(),
                        subasta.getCategoria(),
                        subasta.getMoneda()),
                pujas.size(),
                mayorOferta,
                ganadora.isPresent(),
                ganadora.map(p -> p.getItemCatalogo().getId()).orElse(null),
                importePagado,
                compra.map(registro -> registro.getIdentificador()).orElse(null),
                compra.map(registro -> registro.getEstadoPago()).orElse(null));
    }

    private boolean coincideResultado(SubastaParticipacionDTO item, String resultado) {
        if (resultado == null || resultado.isBlank()) return true;
        String valor = resultado.trim().toLowerCase(Locale.ROOT);
        if ("ganada".equals(valor) || "ganadas".equals(valor)) return item.gano();
        if ("perdida".equals(valor) || "perdidas".equals(valor)) return !item.gano();
        return true;
    }

    private HistorialPujasClienteDTO.SuperadaPorDTO buscarPujaSuperadora(
            Puja puja,
            List<Puja> todasLasPujas) {
        return todasLasPujas.stream()
                .filter(otra -> otra.getItemCatalogo().getId()
                        .equals(puja.getItemCatalogo().getId()))
                .filter(otra -> esPosterior(otra, puja))
                .filter(otra -> valor(otra.getImporte()) > valor(puja.getImporte()))
                .findFirst()
                .map(otra -> new HistorialPujasClienteDTO.SuperadaPorDTO(
                        nombrePostor(otra),
                        otra.getImporte()))
                .orElse(null);
    }

    private boolean esPosterior(Puja candidata, Puja base) {
        if (candidata.getFechaHora() == null || base.getFechaHora() == null) {
            return candidata.getId() > base.getId();
        }
        int comparacion = candidata.getFechaHora().compareTo(base.getFechaHora());
        return comparacion > 0 || (comparacion == 0 && candidata.getId() > base.getId());
    }

    private String nombrePostor(Puja puja) {
        Persona persona = puja.getAsistente().getCliente().getPersona();
        if (persona == null) return "Postor";
        return (persona.getNombre() + " " + persona.getApellido()).trim();
    }

    private String nombreSubasta(Subasta subasta) {
        Catalogo catalogo = subasta.getCatalogo();
        if (catalogo == null || catalogo.getDescription() == null
                || catalogo.getDescription().isBlank()) {
            return "Subasta #" + subasta.getId();
        }
        return catalogo.getDescription();
    }

    private boolean esGanadora(Puja puja) {
        return "si".equalsIgnoreCase(puja.getGanador() == null
                ? "" : puja.getGanador().trim());
    }

    private String normalizarCategoria(String categoria) {
        return categoria == null || categoria.isBlank()
                ? "comun"
                : categoria.trim().toLowerCase(Locale.ROOT);
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
