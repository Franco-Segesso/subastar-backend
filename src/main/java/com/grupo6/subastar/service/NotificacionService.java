package com.grupo6.subastar.service;

import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.Notificacion;
import com.grupo6.subastar.model.TipoNotificacion;
import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Notificacion> obtenerMisNotificaciones(String emailUsuario, Boolean leidas) {
        Cliente cliente = obtenerClientePorEmail(emailUsuario);
        if (leidas != null) {
            return notificacionRepository.findByCliente_IdentificadorAndLeidoOrderByFechaEnvioDesc(cliente.getIdentificador(), leidas);
        }
        return notificacionRepository.findByCliente_IdentificadorOrderByFechaEnvioDesc(cliente.getIdentificador());
    }

    public int marcarTodasComoLeidas(String emailUsuario) {
        Cliente cliente = obtenerClientePorEmail(emailUsuario);
        List<Notificacion> noLeidas = notificacionRepository.findByCliente_IdentificadorAndLeidoFalse(cliente.getIdentificador());

        noLeidas.forEach(n -> n.setLeido(true));
        notificacionRepository.saveAll(noLeidas);

        return noLeidas.size(); // Retornamos la cantidad para el JSON
    }

    public void marcarComoLeida(Integer idNotificacion, String emailUsuario) {
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notificacion.getCliente().getPersona().getEmail().equals(emailUsuario)) {
            throw new RuntimeException("No tienes permiso");
        }

        notificacion.setLeido(true);
        notificacionRepository.save(notificacion);
    }

    // Método utilitario para generar notificaciones desde cualquier otro servicio
    @Transactional
    public void crearNotificacion(Cliente cliente, String titulo, String mensaje, TipoNotificacion tipo, Integer referenciaId) {
        Notificacion nuevaNotif = new Notificacion();
        nuevaNotif.setCliente(cliente);
        nuevaNotif.setTitulo(titulo);
        nuevaNotif.setMensaje(mensaje);
        nuevaNotif.setTipo(tipo);
        nuevaNotif.setReferenciaId(referenciaId);
        nuevaNotif.setLeido(false);
        nuevaNotif.setFechaEnvio(LocalDateTime.now());

        notificacionRepository.saveAndFlush(nuevaNotif);
    }

    // Método auxiliar para no repetir código
    private Cliente obtenerClientePorEmail(String email) {
        return clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

}
