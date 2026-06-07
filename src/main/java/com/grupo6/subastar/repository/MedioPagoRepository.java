package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedioPagoRepository extends JpaRepository<MedioPago, Integer> {
    List<MedioPago> findByClienteIdentificadorAndActivo(Integer clienteId, String activo);
}