package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Asistente;
import com.grupo6.subastar.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AsistenteRepository extends JpaRepository<Asistente, Integer> {
    
    // Valida la regla de concurrencia (One-Slot)
    boolean existsByClienteAndActivo(Cliente cliente, String activo);

    // Recupera al asistente en la sala actual para permitirle pujar
    Optional<Asistente> findByClienteAndSubastaIdAndActivo(Cliente cliente, Integer subastaId, String activo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Asistente a
               SET a.activo = 'no'
             WHERE a.cliente = :cliente
               AND LOWER(TRIM(a.activo)) = 'si'
               AND (a.subasta IS NULL
                    OR LOWER(TRIM(a.subasta.estado)) <> 'abierta')
            """)
    int desactivarSesionesFueraDeSubastaAbierta(@Param("cliente") Cliente cliente);
    
    // Cuenta los asistentes activos que son postores (excluyendo observadores)
    @Query("SELECT COUNT(a) FROM Asistente a WHERE a.subasta.id = :subastaId AND LOWER(TRIM(a.activo)) = 'si' AND (a.numeroPostor IS NULL OR a.numeroPostor <> -1)")
    int countPostoresActivos(@Param("subastaId") Integer subastaId);
}
