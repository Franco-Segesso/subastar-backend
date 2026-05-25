package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.RegistroRequest;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.model.Pais;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void registrarCliente(RegistroRequest req) throws Exception {
        
        // 1. Validar unicidad del email
        if (personaRepository.existsByEmail(req.getEmail())) {
            throw new Exception("El email ya está registrado.");
        }

        // 2. Mapear y guardar la Persona (Padre)
        Persona p = new Persona();
        p.setNombre(req.getNombre());
        p.setApellido(req.getApellido());
        p.setDocumento(req.getDocumento());
        p.setEmail(req.getEmail());
        p.setDireccion(req.getDireccion());
        p.setFechaNacimiento(req.getFechaNacimiento());
        p.setEstado("activo"); 
        p.setFoto(null); 

        personaRepository.save(p);
        personaRepository.flush(); 

        // 3. Mapear y guardar el Cliente (Hijo)
        Cliente c = new Cliente();
        c.setPersona(p); 
        c.setClave(passwordEncoder.encode(req.getClave())); 
        
        // CORRECCIÓN: Asignamos el objeto Pais usando el ID del request
        Pais pais = new Pais();
        pais.setId(req.getNumeroPais());
        c.setPais(pais);

        c.setAdmitido("no"); 
        c.setCategoria("comun"); 
        c.setVerificador("0000"); 
        c.setFechaAprobacion(null); 

        clienteRepository.save(c);
    }
}