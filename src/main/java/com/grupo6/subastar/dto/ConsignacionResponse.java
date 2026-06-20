package com.grupo6.subastar.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConsignacionResponse {
    private Integer identificador;
    private String estado;
    private String motivoRechazo;
    private String motivoDocumentacion;
    private Boolean condicionesAceptadas;
    private LocalDateTime fechaSolicitud;
    private ProductoConsignadoDTO producto;
    private CondicionesEmpresaDTO condicionesEmpresa;
    private UbicacionDepositoDTO ubicacionDeposito;
    private SeguroDTO seguro;
    private List<DocumentoDTO> documentosOrigen;
    private List<InstanciaDTO> instancias;

    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public String getMotivoDocumentacion() { return motivoDocumentacion; }
    public void setMotivoDocumentacion(String motivoDocumentacion) { this.motivoDocumentacion = motivoDocumentacion; }
    public Boolean getCondicionesAceptadas() { return condicionesAceptadas; }
    public void setCondicionesAceptadas(Boolean condicionesAceptadas) { this.condicionesAceptadas = condicionesAceptadas; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public ProductoConsignadoDTO getProducto() { return producto; }
    public void setProducto(ProductoConsignadoDTO producto) { this.producto = producto; }
    public CondicionesEmpresaDTO getCondicionesEmpresa() { return condicionesEmpresa; }
    public void setCondicionesEmpresa(CondicionesEmpresaDTO condicionesEmpresa) { this.condicionesEmpresa = condicionesEmpresa; }
    public UbicacionDepositoDTO getUbicacionDeposito() { return ubicacionDeposito; }
    public void setUbicacionDeposito(UbicacionDepositoDTO ubicacionDeposito) { this.ubicacionDeposito = ubicacionDeposito; }
    public SeguroDTO getSeguro() { return seguro; }
    public void setSeguro(SeguroDTO seguro) { this.seguro = seguro; }
    public List<DocumentoDTO> getDocumentosOrigen() { return documentosOrigen; }
    public void setDocumentosOrigen(List<DocumentoDTO> documentosOrigen) { this.documentosOrigen = documentosOrigen; }
    public List<InstanciaDTO> getInstancias() { return instancias; }
    public void setInstancias(List<InstanciaDTO> instancias) { this.instancias = instancias; }

    public static class ProductoConsignadoDTO {
        private Integer identificador;
        private String tipoBien;
        private String descripcion;
        private String artista;
        private String fechaCreacion;
        private String historia;
        private List<String> fotos;

        public Integer getIdentificador() { return identificador; }
        public void setIdentificador(Integer identificador) { this.identificador = identificador; }
        public String getTipoBien() { return tipoBien; }
        public void setTipoBien(String tipoBien) { this.tipoBien = tipoBien; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getArtista() { return artista; }
        public void setArtista(String artista) { this.artista = artista; }
        public String getFechaCreacion() { return fechaCreacion; }
        public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
        public String getHistoria() { return historia; }
        public void setHistoria(String historia) { this.historia = historia; }
        public List<String> getFotos() { return fotos; }
        public void setFotos(List<String> fotos) { this.fotos = fotos; }
    }

    public static class CondicionesEmpresaDTO {
        private Double precioBase;
        private Double comisionEmpresa;
        private String seguroPoliza;
        private String contactoPoliza;
        private String subastaAsignada;
        private String moneda;

        public Double getPrecioBase() { return precioBase; }
        public void setPrecioBase(Double precioBase) { this.precioBase = precioBase; }
        public Double getComisionEmpresa() { return comisionEmpresa; }
        public void setComisionEmpresa(Double comisionEmpresa) { this.comisionEmpresa = comisionEmpresa; }
        public String getSeguroPoliza() { return seguroPoliza; }
        public void setSeguroPoliza(String seguroPoliza) { this.seguroPoliza = seguroPoliza; }
        public String getContactoPoliza() { return contactoPoliza; }
        public void setContactoPoliza(String contactoPoliza) { this.contactoPoliza = contactoPoliza; }
        public String getSubastaAsignada() { return subastaAsignada; }
        public void setSubastaAsignada(String subastaAsignada) { this.subastaAsignada = subastaAsignada; }
        public String getMoneda() { return moneda; }
        public void setMoneda(String moneda) { this.moneda = moneda; }
    }

    public static class UbicacionDepositoDTO {
        private String nombre;
        private String direccion;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }
    }

    public static class SeguroDTO {
        private String nroPoliza;
        private String compania;
        private Double importe;
        private String polizaCombinada;
        private String moneda;

        public String getNroPoliza() { return nroPoliza; }
        public void setNroPoliza(String nroPoliza) { this.nroPoliza = nroPoliza; }
        public String getCompania() { return compania; }
        public void setCompania(String compania) { this.compania = compania; }
        public Double getImporte() { return importe; }
        public void setImporte(Double importe) { this.importe = importe; }
        public String getPolizaCombinada() { return polizaCombinada; }
        public void setPolizaCombinada(String polizaCombinada) { this.polizaCombinada = polizaCombinada; }
        public String getMoneda() { return moneda; }
        public void setMoneda(String moneda) { this.moneda = moneda; }
    }

    public static class DocumentoDTO {
        private Integer identificador;
        private String nombreArchivo;
        private String urlArchivo;
        private String descripcion;
        private LocalDateTime fechaCarga;
        private String estado;

        public Integer getIdentificador() { return identificador; }
        public void setIdentificador(Integer identificador) { this.identificador = identificador; }
        public String getNombreArchivo() { return nombreArchivo; }
        public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
        public String getUrlArchivo() { return urlArchivo; }
        public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public LocalDateTime getFechaCarga() { return fechaCarga; }
        public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    public static class InstanciaDTO {
        private String titulo;
        private String fecha;
        private Boolean completada;
        private Boolean actual;

        public InstanciaDTO(String titulo, String fecha, Boolean completada, Boolean actual) {
            this.titulo = titulo;
            this.fecha = fecha;
            this.completada = completada;
            this.actual = actual;
        }

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
        public Boolean getCompletada() { return completada; }
        public void setCompletada(Boolean completada) { this.completada = completada; }
        public Boolean getActual() { return actual; }
        public void setActual(Boolean actual) { this.actual = actual; }
    }
}
