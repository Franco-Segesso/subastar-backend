package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.ChequeCertificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChequeCertificadoRepository extends JpaRepository<ChequeCertificado, Integer> {
}