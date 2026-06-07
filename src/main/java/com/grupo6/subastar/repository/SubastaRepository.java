package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Integer> {
    
    @Query("SELECT s FROM Subasta s WHERE " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:categoria IS NULL OR s.categoria = :categoria) AND " +
           "(:moneda IS NULL OR s.moneda = :moneda) AND " +
           "(cast(:fecha as date) IS NULL OR s.fecha = cast(:fecha as date))")
    List<Subasta> findByFiltros(
            @Param("estado") String estado,
            @Param("categoria") String categoria,
            @Param("moneda") String moneda,
            @Param("fecha") LocalDate fecha);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Subasta s SET s.estado = :estado WHERE s.id = :id")
    int actualizarEstado(@Param("id") Integer id, @Param("estado") String estado);

    @Query(value = "SELECT * FROM subastas s WHERE LOWER(LTRIM(RTRIM(s.estado))) = 'pendiente' AND " +
           "(s.fecha < CAST(:fechaActual AS date) OR " +
           "(s.fecha = CAST(:fechaActual AS date) AND s.hora <= CAST(:horaActual AS time)))",
           nativeQuery = true)
    List<Subasta> findPendientesParaAbrir(
            @Param("fechaActual") String fechaActual,
            @Param("horaActual") String horaActual);
}
