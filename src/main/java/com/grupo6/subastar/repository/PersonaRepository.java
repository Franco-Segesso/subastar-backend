package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByEmail(String email);

    boolean existsByEmail(String email);
    
    
    boolean existsByDocumento(String documento);

    
}