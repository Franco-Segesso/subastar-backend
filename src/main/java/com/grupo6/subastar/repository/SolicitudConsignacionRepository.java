package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.SolicitudConsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudConsignacionRepository extends JpaRepository<SolicitudConsignacion, Integer> {

    @Query(value = """
            SELECT s.*
              FROM solicitudesConsignacion s
              JOIN productos p ON p.identificador = s.producto
             WHERE p.duenio = :clienteId
                OR EXISTS (
                    SELECT 1
                      FROM registroDeSubasta r
                     WHERE r.producto = s.producto
                       AND r.duenio = :clienteId
                )
             ORDER BY s.fechaSolicitud DESC
            """, nativeQuery = true)
    List<SolicitudConsignacion> findByClienteId(@Param("clienteId") Integer clienteId);

    @Query(value = """
            SELECT s.*
              FROM solicitudesConsignacion s
              JOIN productos p ON p.identificador = s.producto
             WHERE s.identificador = :id
               AND (
                    p.duenio = :clienteId
                    OR EXISTS (
                        SELECT 1
                          FROM registroDeSubasta r
                         WHERE r.producto = s.producto
                           AND r.duenio = :clienteId
                    )
               )
            """, nativeQuery = true)
    Optional<SolicitudConsignacion> findByIdAndClienteId(@Param("id") Integer id, @Param("clienteId") Integer clienteId);

    @Query(value = """
            SELECT COUNT(DISTINCT s.identificador)
              FROM solicitudesConsignacion s
              JOIN productos p ON p.identificador = s.producto
              LEFT JOIN registroDeSubasta r
                ON r.producto = s.producto
             WHERE p.duenio = :clienteId OR r.duenio = :clienteId
            """, nativeQuery = true)
    long countByClienteId(@Param("clienteId") Integer clienteId);

    @Query("SELECT s FROM SolicitudConsignacion s WHERE s.producto.id = :productoId")
    Optional<SolicitudConsignacion> findByProductoId(@Param("productoId") Integer productoId);

    @Query("SELECT s.identificador FROM SolicitudConsignacion s WHERE s.producto.id = :productoId")
    Optional<Integer> findIdByProductoId(@Param("productoId") Integer productoId);
}
