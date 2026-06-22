package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MultaRepository extends JpaRepository<Multa, Integer> {
    List<Multa> findByClienteIdentificadorOrderByFechaGeneracionDesc(Integer clienteId);
    Optional<Multa> findByIdentificadorAndClienteIdentificador(Integer id, Integer clienteId);
    boolean existsByPujaId(Integer pujaId);
    boolean existsByClienteIdentificadorAndEstadoIgnoreCase(Integer clienteId, String estado);
    long countByClienteIdentificadorAndEstadoIgnoreCase(Integer clienteId, String estado);
    List<Multa> findByFechaVencimientoLessThanEqual(LocalDateTime fecha);

}
