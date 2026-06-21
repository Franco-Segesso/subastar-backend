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

    @Query("""
            SELECT s FROM SolicitudConsignacion s
            WHERE s.producto.duenio = :clienteId
               OR EXISTS (
                    SELECT r.identificador FROM RegistroSubasta r
                    WHERE r.productoId = s.producto.id
                      AND r.duenioId = :clienteId
               )
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudConsignacion> findByClienteId(@Param("clienteId") Integer clienteId);

    @Query("""
            SELECT s FROM SolicitudConsignacion s
            WHERE s.identificador = :id
              AND (
                    s.producto.duenio = :clienteId
                    OR EXISTS (
                        SELECT r.identificador FROM RegistroSubasta r
                        WHERE r.productoId = s.producto.id
                          AND r.duenioId = :clienteId
                    )
              )
            """)
    Optional<SolicitudConsignacion> findByIdAndClienteId(@Param("id") Integer id, @Param("clienteId") Integer clienteId);

    @Query("""
            SELECT COUNT(s) FROM SolicitudConsignacion s
            WHERE s.producto.duenio = :clienteId
               OR EXISTS (
                    SELECT r.identificador FROM RegistroSubasta r
                    WHERE r.productoId = s.producto.id
                      AND r.duenioId = :clienteId
               )
            """)
    long countByClienteId(@Param("clienteId") Integer clienteId);

    @Query("SELECT s FROM SolicitudConsignacion s WHERE s.producto.id = :productoId")
    Optional<SolicitudConsignacion> findByProductoId(@Param("productoId") Integer productoId);

    @Query("SELECT s.identificador FROM SolicitudConsignacion s WHERE s.producto.id = :productoId")
    Optional<Integer> findIdByProductoId(@Param("productoId") Integer productoId);
}
