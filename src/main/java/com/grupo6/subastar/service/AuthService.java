package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.ActivarRequest;
import com.grupo6.subastar.dto.RegistroRequest;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.model.Pais;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.PaisRepository;
import com.grupo6.subastar.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthService {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PaisRepository paisRepository;

    @Transactional
    public void registrarCliente(RegistroRequest req, MultipartFile fotoFrente, MultipartFile fotoDorso) throws Exception {

        // 1. Validar unicidad del email
        if (personaRepository.existsByEmail(req.getEmail())) {
            throw new Exception("El email ya está registrado.");
        }

        if (personaRepository.existsByDocumento(req.getDocumento())) {
            throw new Exception("El documento de identidad ya se encuentra registrado.");
        }

        // 2. Buscar el país en la base de datos ANTES de armar los objetos
        Pais paisReal = paisRepository.findById(req.getNumeroPais())
                .orElseThrow(() -> new Exception("El país seleccionado no existe en la base de datos"));

        // 3. Mapear y guardar la Persona
        Persona p = new Persona();
        p.setNombre(req.getNombre());
        p.setApellido(req.getApellido());
        p.setDocumento(req.getDocumento());
        p.setEmail(req.getEmail());
        p.setDireccion(req.getDireccion());
        p.setFechaNacimiento(req.getFechaNacimiento());
        p.setEstado("activo");

        if (fotoFrente != null && !fotoFrente.isEmpty()) {
            String urlFrente = cloudinaryService.subirImagen(fotoFrente);
            p.setFotoFrente(urlFrente);
        }
        if (fotoDorso != null && !fotoDorso.isEmpty()) {
            String urlDorso = cloudinaryService.subirImagen(fotoDorso);
            p.setFotoDorso(urlDorso);
        }

        // Guardamos la Persona primero para que se le genere el Identificador (ID)
        personaRepository.save(p);
        personaRepository.flush();

        // 4. Mapear y guardar el Cliente (Hijo)
        Cliente c = new Cliente();
        c.setPersona(p);
        c.setPais(paisReal);

        c.setAdmitido("pendiente");
        c.setCategoria("comun");
        c.setVerificadorId(1);
        c.setFechaAprobacion(null);

        // Guardamos el Cliente
        clienteRepository.save(c);
    }

   @Transactional
    public void activarCuenta(ActivarRequest req) throws Exception {


        // Regla: Mínimo 8 caracteres, 1 número, 1 minúscula, 1 mayúscula, 1 símbolo.
        String patronPassword = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*._-]).{8,}$";
        if (!req.getClave().matches(patronPassword)) {
            throw new Exception("La contraseña debe tener mín. 8 caracteres, una mayúscula, una minúscula, un número y un símbolo especial.");
        }
        // ---------------------------------------------

        if (!req.getClave().equals(req.getClaveConfirmacion())) {
            throw new Exception("Las claves no coinciden.");
        }

        Persona p = personaRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new Exception("No existe una persona con ese email."));

        Cliente c = clienteRepository.findById(p.getIdentificador())
                .orElseThrow(() -> new Exception("No existe un cliente asociado."));

        // 1. Validamos que el administrador ya lo haya admitido
        if (!"si".equalsIgnoreCase(c.getAdmitido())) {
            throw new Exception("Cuenta pendiente de aprobación por la empresa.");
        }

        // 2. Si ya tiene clave, es que ya completó el registro
        if (c.getClave() != null && !c.getClave().isEmpty()) {
            throw new Exception("El usuario ya ha completado su registro anteriormente.");
        }

        // 3. Si llega hasta acá, es la primera vez que activa su cuenta
        c.setClave(passwordEncoder.encode(req.getClave()));
        clienteRepository.save(c);
    }
}
