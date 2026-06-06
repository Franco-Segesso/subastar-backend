package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Puja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PujaRepository extends JpaRepository<Puja, Integer> {

    // Busca la mejor oferta recorriendo: Puja -> ItemCatalogo -> Catalogo -> Subasta
    @Query("SELECT MAX(p.importe) FROM Puja p WHERE p.itemCatalogo.catalogo.subasta.id = :idSubasta")
    Double findMaxImporteBySubastaId(@Param("idSubasta") Integer idSubasta);

    // Cuenta los postores recorriendo el mismo camino
    @Query("SELECT COUNT(DISTINCT p.asistente.cliente.id) FROM Puja p WHERE p.itemCatalogo.catalogo.subasta.id = :idSubasta")
    Integer countDistinctPostoresBySubastaId(@Param("idSubasta") Integer idSubasta);
}