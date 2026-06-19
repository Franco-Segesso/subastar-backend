package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Todas ordenadas por fecha
    List<Notificacion> findByCliente_IdentificadorOrderByFechaEnvioDesc(Integer clienteId);

    // Filtradas por estado (leido = true/false) ordenadas por fecha
    List<Notificacion> findByCliente_IdentificadorAndLeidoOrderByFechaEnvioDesc(Integer clienteId, Boolean leido);

    // Para el PATCH masivo
    List<Notificacion> findByCliente_IdentificadorAndLeidoFalse(Integer clienteId);
}
