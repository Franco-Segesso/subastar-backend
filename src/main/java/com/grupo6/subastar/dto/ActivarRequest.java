package com.grupo6.subastar.dto;

public class ActivarRequest {
    private String email;
    private String tokenActivacion;
    private String clave;
    private String claveConfirmacion;

    public ActivarRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTokenActivacion() { return tokenActivacion; }
    public void setTokenActivacion(String tokenActivacion) { this.tokenActivacion = tokenActivacion; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getClaveConfirmacion() { return claveConfirmacion; }
    public void setClaveConfirmacion(String claveConfirmacion) { this.claveConfirmacion = claveConfirmacion; }
}