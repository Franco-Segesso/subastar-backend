package com.grupo6.subastar.dto;

import java.time.LocalDate;

public class MedioPagoDTO {
    private Integer identificador;
    private String tipo;
    private String verificado;
    private String activo;
    private LocalDate fechaAlta;

    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getVerificado() { return verificado; }
    public void setVerificado(String verificado) { this.verificado = verificado; }
    public String getActivo() { return activo; }
    public void setActivo(String activo) { this.activo = activo; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
}