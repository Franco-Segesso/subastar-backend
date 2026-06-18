package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.RegistroSubasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistroSubastaRepository extends JpaRepository<RegistroSubasta, Integer> {
    Optional<RegistroSubasta> findByIdentificadorAndClienteId(Integer identificador, Integer clienteId);
    Optional<RegistroSubasta> findFirstBySubastaIdAndProductoIdAndClienteId(
            Integer subastaId,
            Integer productoId,
            Integer clienteId);
}
