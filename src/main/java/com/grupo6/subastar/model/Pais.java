package com.grupo6.subastar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "paises")
public class Pais {

    @Id
    @Column(name = "numero")
    private Integer id;

    private String nombre;
    
    @Column(name = "nombreCorto")
    private String nombreCorto;
    
    private String capital;
    private String nacionalidad;
    private String idiomas;

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreCorto() { return nombreCorto; }
    public void setNombreCorto(String nombreCorto) { this.nombreCorto = nombreCorto; }
    public String getCapital() { return capital; }
    public void setCapital(String capital) { this.capital = capital; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
    public String getIdiomas() { return idiomas; }
    public void setIdiomas(String idiomas) { this.idiomas = idiomas; }
}