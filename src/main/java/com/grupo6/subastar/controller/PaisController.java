package com.grupo6.subastar.controller;

import com.grupo6.subastar.model.Pais;
import com.grupo6.subastar.repository.PaisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/paises")
public class PaisController {

    @Autowired
    private PaisRepository paisRepository;

    @GetMapping
    public ResponseEntity<List<Pais>> obtenerPaises() {
        List<Pais> listaPaises = paisRepository.findAll();
        return ResponseEntity.ok(listaPaises);
    }
}