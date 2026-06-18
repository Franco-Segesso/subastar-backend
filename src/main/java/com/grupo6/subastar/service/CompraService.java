package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.CompraDTO;
import com.grupo6.subastar.dto.ModalidadEntregaResponse;
import com.grupo6.subastar.model.Catalogo;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Producto;
import com.grupo6.subastar.model.RegistroSubasta;
import com.grupo6.subastar.model.Subasta;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.ItemCatalogoRepository;
import com.grupo6.subastar.repository.ProductoRepository;
import com.grupo6.subastar.repository.RegistroSubastaRepository;
import com.grupo6.subastar.repository.SubastaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CompraService {

    private final ClienteRepository clienteRepository;
    private final RegistroSubastaRepository registroRepository;
    private final SubastaRepository subastaRepository;
    private final ProductoRepository productoRepository;
    private final ItemCatalogoRepository itemRepository;

    public CompraService(
            ClienteRepository clienteRepository,
            RegistroSubastaRepository registroRepository,
            SubastaRepository subastaRepository,
            ProductoRepository productoRepository,
            ItemCatalogoRepository itemRepository) {
        this.clienteRepository = clienteRepository;
        this.registroRepository = registroRepository;
        this.subastaRepository = subastaRepository;
        this.productoRepository = productoRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public CompraDTO obtenerCompra(String email, Integer compraId) {
        Cliente cliente = obtenerCliente(email);
        RegistroSubasta compra = registroRepository.findById(compraId)
                .orElseThrow(() -> new RuntimeException("404: Compra inexistente"));
        if (!cliente.getIdentificador().equals(compra.getClienteId())) {
            throw new RuntimeException("403: La compra no pertenece al cliente");
        }

        Subasta subasta = subastaRepository.findById(compra.getSubastaId())
                .orElseThrow(() -> new RuntimeException("404: Subasta inexistente"));
        Producto producto = productoRepository.findById(compra.getProductoId())
                .orElseThrow(() -> new RuntimeException("404: Producto inexistente"));
        ItemCatalogo item = itemRepository.findBySubastaIdAndProductoId(
                        compra.getSubastaId(), compra.getProductoId())
                .orElseThrow(() -> new RuntimeException("404: Item inexistente"));

        double costoEnvio = compra.getCostoEnvio() == null ? 0.0 : compra.getCostoEnvio();
        double total = valor(compra.getImporte()) + valor(compra.getComision()) + costoEnvio;
        String modalidad = normalizarModalidad(compra.getModalidadEntrega());
        String direccion = "envio".equals(modalidad) && cliente.getPersona() != null
                ? cliente.getPersona().getDireccion()
                : null;

        return new CompraDTO(
                compra.getIdentificador(),
                new CompraDTO.SubastaDTO(
                        subasta.getId(),
                        nombreSubasta(subasta),
                        subasta.getMoneda()),
                new CompraDTO.ItemDTO(
                        item.getId(),
                        item.getId(),
                        producto.getDescripcion()),
                compra.getImporte(),
                compra.getComision(),
                compra.getCostoEnvio(),
                total,
                modalidad,
                direccion,
                avisoSeguro(modalidad, compra.getNroPolizaSeguro()));
    }

    @Transactional
    public ModalidadEntregaResponse definirEntrega(
            String email,
            Integer compraId,
            String modalidadSolicitada) {
        Cliente cliente = obtenerCliente(email);
        RegistroSubasta compra = registroRepository.findById(compraId)
                .orElseThrow(() -> new RuntimeException("404: Compra inexistente"));
        if (!cliente.getIdentificador().equals(compra.getClienteId())) {
            throw new RuntimeException("403: La compra no pertenece al cliente");
        }

        String modalidad = normalizarModalidad(modalidadSolicitada);
        if (!"envio".equals(modalidad) && !"retiro".equals(modalidad)) {
            throw new RuntimeException("400: Modalidad invalida");
        }
        if (!"pendiente".equals(normalizarModalidad(compra.getModalidadEntrega()))) {
            throw new RuntimeException("409: La entrega ya fue definida");
        }

        compra.setModalidadEntrega(modalidad);
        if ("retiro".equals(modalidad)) {
            compra.setCostoEnvio(0.0);
        }
        registroRepository.save(compra);
        return new ModalidadEntregaResponse(
                "Modalidad registrada correctamente.",
                modalidad);
    }

    private Cliente obtenerCliente(String email) {
        return clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException("401: Token invalido, ausente o expirado"));
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
        if ("retiro".equals(modalidad)) {
            return "Al retirar el bien personalmente, la cobertura del seguro finaliza en el momento de la entrega.";
        }
        if (poliza == null || poliza.isBlank()) {
            return "La cobertura del seguro se informara junto con la entrega.";
        }
        return "El bien permanece cubierto por la poliza " + poliza + " hasta su entrega.";
    }

    private String normalizarModalidad(String modalidad) {
        return modalidad == null || modalidad.isBlank()
                ? "pendiente"
                : modalidad.trim().toLowerCase(Locale.ROOT);
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
