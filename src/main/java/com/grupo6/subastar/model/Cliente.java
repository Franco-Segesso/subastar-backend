package com.grupo6.subastar.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    private Integer identificador;

    @OneToOne
    @MapsId
    @JoinColumn(name = "identificador")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numeroPais")
    private Pais pais;

    // constraint chkAdmitido check(admitido in ('si','no','pendiente'))
    @Column(name = "admitido", length = 10)
    private String admitido;

    // constraint chkCategoria check (categoria in ('comun', 'especial', 'plata', 'oro', 'platino'))
    @Column(name = "categoria", length = 10)
    private String categoria;

    // FK temporal hacia empleados hasta que el equipo cree la entidad Empleado
    @Column(name = "verificador", nullable = false)
    private Integer verificadorId;

    @Column(name = "clave", length = 250)
    private String clave;

    @Column(name = "fechaAprobacion")
    private LocalDate fechaAprobacion;

   

    

   

    // Constructores
    public Cliente() {}

    // Getters y Setters
    public Integer getIdentificador() { return identificador; }
    public void setIdentificador(Integer identificador) { this.identificador = identificador; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Pais getPais() { return pais; }
    public void setPais(Pais pais) { this.pais = pais; }

    public String getAdmitido() { return admitido; }
    public void setAdmitido(String admitido) { this.admitido = admitido; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getVerificadorId() { return verificadorId; }
    public void setVerificadorId(Integer verificadorId) { this.verificadorId = verificadorId; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public LocalDate getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDate fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }

    

    
    

    
}