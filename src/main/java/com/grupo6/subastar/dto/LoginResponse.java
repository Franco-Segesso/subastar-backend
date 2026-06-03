package com.grupo6.subastar.dto;

public class LoginResponse {
    private String token;
    private ClienteDTO cliente;

    public LoginResponse(String token, ClienteDTO cliente) {
        this.token = token;
        this.cliente = cliente;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public ClienteDTO getCliente() { return cliente; }
    public void setCliente(ClienteDTO cliente) { this.cliente = cliente; }
}