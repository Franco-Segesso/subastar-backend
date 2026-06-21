package com.grupo6.subastar.repository;

import com.grupo6.subastar.model.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeguroRepository extends JpaRepository<Seguro, String> {
}
