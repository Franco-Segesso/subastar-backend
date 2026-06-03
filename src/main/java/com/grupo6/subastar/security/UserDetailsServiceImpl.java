package com.grupo6.subastar.security;

import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Persona;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscamos a la persona por email
        Persona persona = personaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // 2. Buscamos el cliente asociado a esa persona (comparten el mismo ID)
        Cliente cliente = clienteRepository.findById(persona.getIdentificador())
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró un cliente para el email: " + email));

        // 3. Devolvemos nuestro UserDetailsImpl
        return new UserDetailsImpl(cliente);
    }
}