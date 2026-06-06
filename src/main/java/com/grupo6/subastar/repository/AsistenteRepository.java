package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Asistente;
import com.grupo6.subastar.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AsistenteRepository extends JpaRepository<Asistente, Integer> {
    
    // Valida la regla de concurrencia (One-Slot)
    boolean existsByClienteAndActivo(Cliente cliente, String activo);

    // Recupera al asistente en la sala actual para permitirle pujar
    Optional<Asistente> findByClienteAndSubastaIdAndActivo(Cliente cliente, Integer subastaId, String activo);
}