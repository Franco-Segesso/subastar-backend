package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.CuentaDestino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaDestinoRepository extends JpaRepository<CuentaDestino, Integer> {

    Optional<CuentaDestino> findBySolicitudIdentificadorAndActiva(Integer solicitudId, String activa);

    boolean existsBySolicitudIdentificadorAndActiva(Integer solicitudId, String activa);

    Optional<CuentaDestino> findBySolicitudProductoIdAndActiva(Integer productoId, String activa);
}
