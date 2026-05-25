package com.grupo6.subastar.dto;

import java.time.LocalDate;

public class RegistroRequest {
    private String nombre;
    private String apellido;
    private String documento;
    private String email;
    private String clave;
    private String direccion;
    private LocalDate fechaNacimiento;
    private Integer numeroPais;

    // Constructores
    public RegistroRequest() {}

    // Getters
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getDocumento() { return documento; }
    public String getEmail() { return email; }
    public String getClave() { return clave; }
    public String getDireccion() { return direccion; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public Integer getNumeroPais() { return numeroPais; }
}