package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "pujos")
public class Puja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer id;

    @Column(name = "ganador")
    private String ganador;

    @Column(name = "importe", nullable = false)
    private Double importe;

    @Column(name = "fechaHora") 
    private LocalDateTime fechaHora;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item", referencedColumnName = "identificador")
    private ItemCatalogo itemCatalogo;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asistente", referencedColumnName = "identificador")
    private Asistente asistente;

    public Puja() {
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getGanador() { return ganador; }
    public void setGanador(String ganador) { this.ganador = ganador; }

    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public ItemCatalogo getItemCatalogo() { return itemCatalogo; }
    public void setItemCatalogo(ItemCatalogo itemCatalogo) { this.itemCatalogo = itemCatalogo; }

    public Asistente getAsistente() { return asistente; }
    public void setAsistente(Asistente asistente) { this.asistente = asistente; }
}