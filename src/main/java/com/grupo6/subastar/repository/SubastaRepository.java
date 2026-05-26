package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Integer> {
    
    @Query("SELECT s FROM Subasta s WHERE " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:categoria IS NULL OR s.categoria = :categoria) AND " +
           "(:moneda IS NULL OR s.moneda = :moneda)")
    List<Subasta> findByFiltros(
            @Param("estado") String estado,
            @Param("categoria") String categoria,
            @Param("moneda") String moneda);
}