package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.ItemCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Integer> {
    
    // Busca un ítem asegurando que pertenezca al catálogo de la subasta indicada
    @Query("SELECT i FROM ItemCatalogo i WHERE i.id = :itemId AND i.catalogo.subasta.id = :subastaId")
    Optional<ItemCatalogo> findByIdAndSubastaId(
            @Param("subastaId") Integer subastaId, 
            @Param("itemId") Integer itemId);

    // Cuenta cuántos ítems de una subasta específica tienen un estado particular (ej: "no")
    @Query("SELECT COUNT(ic) FROM ItemCatalogo ic WHERE ic.catalogo.subasta.id = :subastaId AND " +
            "(ic.subastado IS NULL OR LOWER(TRIM(ic.subastado)) IN ('no', 'false', 'pendiente'))")
    long countPendientesBySubastaId(@Param("subastaId") Integer subastaId);

    @Query("SELECT ic FROM ItemCatalogo ic WHERE ic.catalogo.subasta.id = :subastaId AND " +
            "(ic.subastado IS NULL OR LOWER(TRIM(ic.subastado)) IN ('no', 'false', 'pendiente')) ORDER BY ic.id ASC")
    List<ItemCatalogo> findPendientesBySubastaId(@Param("subastaId") Integer subastaId);

    @Query("SELECT i FROM ItemCatalogo i WHERE i.catalogo.subasta.id = :subastaId AND i.producto.id = :productoId")
    Optional<ItemCatalogo> findBySubastaIdAndProductoId(
            @Param("subastaId") Integer subastaId,
            @Param("productoId") Integer productoId);

    @Query("SELECT i FROM ItemCatalogo i WHERE i.producto.id = :productoId ORDER BY i.id DESC")
    List<ItemCatalogo> findByProductoIdOrderByIdDesc(@Param("productoId") Integer productoId);
}
