package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.DocumentoConsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoConsignacionRepository
        extends JpaRepository<DocumentoConsignacion, Integer> {

    List<DocumentoConsignacion> findBySolicitudIdentificadorOrderByFechaCargaAsc(
            Integer solicitudId);
}
