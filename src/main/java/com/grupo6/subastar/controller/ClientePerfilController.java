package com.grupo6.subastar.controller;

import com.grupo6.subastar.dto.ClienteDTO;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.security.UserDetailsImpl;
import com.grupo6.subastar.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/clientes")
public class ClientePerfilController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PatchMapping(value = "/me/foto", consumes = {"multipart/form-data"})
    public ResponseEntity<?> actualizarFotoPerfil(
            Authentication authentication,
            @RequestParam("foto") MultipartFile foto
    ) {
        try {
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.badRequest().body("La foto es obligatoria.");
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            Cliente cliente = clienteRepository.findById(userDetails.getCliente().getIdentificador())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado."));

            String urlFoto = cloudinaryService.subirImagen(foto);

            cliente.getPersona().setFotoPerfil(urlFoto);
            clienteRepository.save(cliente);

            ClienteDTO dto = new ClienteDTO();
            dto.setIdentificador(cliente.getIdentificador());
            dto.setNombre(cliente.getPersona().getNombre());
            dto.setApellido(cliente.getPersona().getApellido());
            dto.setEmail(cliente.getPersona().getEmail());
            dto.setCategoria(cliente.getCategoria());
            dto.setAdmitido(cliente.getAdmitido());
            dto.setDocumento(cliente.getPersona().getDocumento());
            dto.setDireccion(cliente.getPersona().getDireccion());
            dto.setPais(cliente.getPais() != null ? cliente.getPais().getNombre() : "Sin país");
            dto.setFoto(cliente.getPersona().getFotoPerfil());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se pudo actualizar la foto: " + e.getMessage());
        }
    }
}