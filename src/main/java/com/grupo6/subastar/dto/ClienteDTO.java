package com.grupo6.subastar.dto;

public class ClienteDTO {
    private Integer identificador;
    private String nombre;
    private String apellido;
    private String email;
    private String categoria;
    private String admitido;
    private String documento;
    private String direccion;
    private String pais;

    // Getters y Setters
    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getAdmitido() { return admitido; }
    public void setAdmitido(String admitido) { this.admitido = admitido; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
}