package com.grupo6.subastar.security;

import com.grupo6.subastar.model.Cliente;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Cliente cliente;

    public UserDetailsImpl(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Le asignamos un rol genérico. A futuro podríamos mapear la "categoria" (plata, oro, etc) aquí.
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return cliente.getClave();
    }

    @Override
    public String getUsername() {
        return cliente.getPersona().getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Bloqueamos la cuenta si el admitido es explícitamente "no" o "pendiente"
        // Según el script SQL: admitido in ('si','no','pendiente')
        return "si".equalsIgnoreCase(cliente.getAdmitido());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Bloqueamos la cuenta si el estado es "inactivo"
        // Según el script SQL: estado in ('activo', 'inactivo')
        return "activo".equalsIgnoreCase(cliente.getPersona().getEstado());
    }

    public Cliente getCliente() {
        return cliente;
    }
}