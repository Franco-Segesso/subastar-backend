package com.grupo6.subastar.dto;

public class RegistroRequest {
    private String nombre;
    private String apellido;
    private String documento;
    private String email;
    private String direccion;
    private Integer numeroPais;
    private java.time.LocalDate fechaNacimiento;

    public RegistroRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public Integer getNumeroPais() { return numeroPais; }
    public void setNumeroPais(Integer numeroPais) { this.numeroPais = numeroPais; }
    public java.time.LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.time.LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

}