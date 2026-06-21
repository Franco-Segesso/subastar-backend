package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.RegistroSubasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface RegistroSubastaRepository extends JpaRepository<RegistroSubasta, Integer> {
    Optional<RegistroSubasta> findByIdentificadorAndClienteId(Integer identificador, Integer clienteId);
    Optional<RegistroSubasta> findFirstBySubastaIdAndProductoIdAndClienteId(
            Integer subastaId,
            Integer productoId,
            Integer clienteId);

    Optional<RegistroSubasta> findFirstByProductoIdAndEstadoPago(Integer productoId, String estadoPago);

    @Query("SELECT COALESCE(SUM(r.importe + r.comision + COALESCE(r.costoEnvio, 0)), 0) " +
            "FROM RegistroSubasta r WHERE r.medioPagoId = :medioPagoId " +
            "AND LOWER(r.estadoPago) = 'pendiente'")
    Double sumPendienteByMedioPagoId(@Param("medioPagoId") Integer medioPagoId);

    Integer countByClienteId(Integer clienteId);
}
