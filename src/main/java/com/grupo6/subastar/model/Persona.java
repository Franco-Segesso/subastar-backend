package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(name = "documento", nullable = false, length = 20)
    private String documento;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 150)
    private String apellido;

    @Column(name = "email", nullable = false, unique = true, length = 250)
    private String email;

    @Column(name = "fechaNacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", length = 250)
    private String direccion;

    // constraint chkEstado check (estado in ('activo', 'inactivo'))
    @Column(name = "estado", length = 15) 
    private String estado;

    @Column(name = "fotoFrente", length = 500)
    private String fotoFrente;

    @Column(name = "fotoDorso", length = 500)
    private String fotoDorso;

    // Constructores
    public Persona() {}

    // Getters y Setters
    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // No te olvides de agregar los Getters y Setters para fotoFrente y fotoDorso abajo de todo
    public String getFotoFrente() { return fotoFrente; }
    public void setFotoFrente(String fotoFrente) { this.fotoFrente = fotoFrente; }

    public String getFotoDorso() { return fotoDorso; }
    public void setFotoDorso(String fotoDorso) { this.fotoDorso = fotoDorso; }
}