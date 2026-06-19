package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.ItemCatalogo;
import com.grupo6.subastar.model.Puja;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface PujaRepository extends JpaRepository<Puja, Integer> {

    List<Puja> findByGanadorIgnoreCaseAndFechaHoraLessThanEqual(
            String ganador,
            LocalDateTime fechaHora);

    // Busca la mejor oferta recorriendo: Puja -> ItemCatalogo -> Catalogo -> Subasta
    @Query("SELECT MAX(p.importe) FROM Puja p WHERE p.itemCatalogo.catalogo.subasta.id = :idSubasta")
    Double findMaxImporteBySubastaId(@Param("idSubasta") Integer idSubasta);

    // Cuenta los postores recorriendo el mismo camino
    @Query("SELECT COUNT(DISTINCT p.asistente.cliente.id) FROM Puja p WHERE p.itemCatalogo.catalogo.subasta.id = :idSubasta")
    Integer countDistinctPostoresBySubastaId(@Param("idSubasta") Integer idSubasta);

    // Recupera la puja más alta para un ítem en particular
    Optional<Puja> findTopByItemCatalogoOrderByImporteDesc(ItemCatalogo item);

    List<Puja> findByItemCatalogoOrderByFechaHoraDesc(ItemCatalogo item);

    @Query("SELECT p FROM Puja p " +
            "JOIN FETCH p.asistente a " +
            "JOIN FETCH a.cliente c " +
            "JOIN FETCH c.persona " +
            "JOIN FETCH a.subasta s " +
            "JOIN FETCH p.itemCatalogo i " +
            "JOIN FETCH i.catalogo ca " +
            "WHERE c.identificador = :clienteId " +
            "ORDER BY s.fecha DESC, s.hora DESC, p.fechaHora ASC, p.id ASC")
    List<Puja> findHistorialByClienteId(@Param("clienteId") Integer clienteId);

    @Query("SELECT p FROM Puja p " +
            "JOIN FETCH p.asistente a " +
            "JOIN FETCH a.cliente c " +
            "JOIN FETCH c.persona " +
            "JOIN FETCH p.itemCatalogo i " +
            "JOIN FETCH i.catalogo ca " +
            "WHERE ca.subasta.id = :subastaId " +
            "ORDER BY p.fechaHora ASC, p.id ASC")
    List<Puja> findHistorialBySubastaId(@Param("subastaId") Integer subastaId);
}
