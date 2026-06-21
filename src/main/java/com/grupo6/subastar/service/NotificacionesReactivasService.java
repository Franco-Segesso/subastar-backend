package com.grupo6.subastar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.TipoNotificacion;
import com.grupo6.subastar.repository.ClienteRepository;

@Service
public class NotificacionesReactivasService {

    @Autowired
    private FirebasePushService firebasePushService;
    
    @Autowired
    private NotificacionService notificacionService;
    
    @Autowired
    private ClienteRepository clienteRepository;

    // =================================================================================
    // 1. EL CLIENTE GANA EN LA SALA DE PUJA (Desde Backend)
    // =================================================================================
    public void notificarSubastaGanada(Cliente cliente, String nombreItem, double pujaGanadora, double comisiones, double costoEnvio, Integer referenciaId) {
        double totalAPagar = pujaGanadora + comisiones + costoEnvio;
        
        String titulo = "¡Subasta Ganada!";
        String mensajePush = "Ganaste '" + nombreItem + "'. Total a pagar: $" + totalAPagar + ". Toca para ver el detalle.";
        
        String detalleBd = String.format(
            "¡Felicidades! Eres el nuevo dueño de '%s'.\n\nDesglose de pago:\n- Valor pujado: $%.2f\n- Comisiones: $%.2f\n- Costo de envío: $%.2f\n\nTOTAL A ABONAR: $%.2f",
            nombreItem, pujaGanadora, comisiones, costoEnvio, totalAPagar
        );

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.GANADA, referenciaId);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 2. NOTIFICAR AL VENDEDOR QUE SU BIEN SE VENDIÓ (Desde Backend)
    // =================================================================================
    public void notificarBienVendidoAlDuenio(Integer idDuenio, String nombreBien, double precioFinal, double comisionCasa, Integer referenciaId) {
        Cliente duenio = clienteRepository.findById(idDuenio)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idDuenio));
        
        double netoAGanar = precioFinal - comisionCasa;
        
        String titulo = "¡Tu artículo ha sido vendido!";
        String mensajePush = "Tu bien '" + nombreBien + "' se vendió por $" + precioFinal + ". Revisa el detalle de la liquidación.";
        
        String detalleBd = String.format(
            "¡Buenas noticias! Tu artículo '%s' se vendió exitosamente en la subasta.\n\n" +
            "Detalle de liquidación:\n- Precio de martillo: $%.2f\n- Comisión de la casa: -$%.2f\n\n" +
            "Monto neto a transferir: $%.2f\n\nEl dinero será enviado a la cuenta a la vista que declaraste previamente.",
            nombreBien, precioFinal, comisionCasa, netoAGanar
        );

        notificacionService.crearNotificacion(duenio, titulo, detalleBd, TipoNotificacion.GANADA, referenciaId);
        firebasePushService.enviarNotificacionPush(duenio.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 3. EL CLIENTE CONSIGNA UN BIEN (Desde Backend)
    // =================================================================================
    public void notificarConsignacionEnviada(Cliente cliente, String nombreBien, Integer referenciaId) {
        String titulo = "Consignación en Revisión";
        String mensaje = "Hemos recibido el formulario de tu artículo '" + nombreBien + "'. Nuestros expertos lo están evaluando.";

        notificacionService.crearNotificacion(cliente, titulo, mensaje, TipoNotificacion.INFORMATIVA, referenciaId);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensaje);
    }

    // =================================================================================
    // 4. EL BIEN ES RECHAZADO POR LA CASA (Vía Script)
    // =================================================================================
    public void notificarBienRechazado(Integer idCliente, String nombreBien, String motivoRechazo, Integer referenciaId) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));

        String titulo = "Actualización de Consignación";
        String mensajePush = "Lamentablemente tu artículo '" + nombreBien + "' no fue aceptado. Toca para ver los detalles.";
        
        String detalleBd = "Tras la inspección, tu artículo '" + nombreBien + "' ha sido rechazado.\n\nMotivo: " + motivoRechazo + 
                           "\n\nTe recordamos que, tal como aceptaste en los términos, el bien te será devuelto a tu domicilio con cargo a tu cuenta.";

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.CONSIGNACION, referenciaId);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 5. EL BIEN ES ACEPTADO - SE LE ENVÍA LA OFERTA AL CLIENTE (Vía Script)
    // =================================================================================
    public void notificarOfertaDeLaCasa(Integer idCliente, String nombreBien, double valorBase, double comisionPorcentaje, String fecha, String hora, String lugar, Integer referenciaId) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));

        String titulo = "¡Artículo Aceptado!";
        String mensajePush = "Tenemos una propuesta para subastar tu '" + nombreBien + "'. Revisa los términos.";
        
        String detalleBd = String.format(
            "¡Tu artículo '%s' ha sido aprobado para subasta!\n\nCondiciones propuestas:\n- Valor Base: $%.2f\n- Comisión de la casa: %.1f%%\n\nLa subasta se realizará el %s a las %s en %s.\n\nPor favor, ingresa a la app para ACEPTAR o RECHAZAR estas condiciones.",
            nombreBien, valorBase, comisionPorcentaje, fecha, hora, lugar
        );

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.CONSIGNACION, referenciaId);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 6. EL CLIENTE RECHAZA LA OFERTA DE LA CASA (Desde Backend)
    // =================================================================================
    public void notificarOfertaRechazadaPorCliente(Cliente cliente, String nombreBien, String sucursalRetiro, double cargoDevolucion, Integer referenciaId) {
        String titulo = "Devolución de Artículo";
        String mensajePush = "Has rechazado nuestra propuesta para '" + nombreBien + "'.";
        
        String detalleBd = String.format(
            "Lamentamos que no hayas aceptado la propuesta de valor base y comisiones para tu '%s'.\n\nPuedes pasar a retirar tu artículo por nuestra sucursal ubicada en %s.\nRecuerda que debes abonar un cargo operativo de $%.2f al momento del retiro.",
            nombreBien, sucursalRetiro, cargoDevolucion
        );

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.INFORMATIVA, referenciaId);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 7. PEDIDO DE DOCUMENTACIÓN (Vía Script)
    // =================================================================================
    public void notificarPedidoDocumentacion(Integer idCliente, String nombreBien, Integer referenciaId) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));

        String titulo = "Documentación Requerida";
        String mensajePush = "Necesitamos verificar el origen de '" + nombreBien + "'. Toca para más info.";
        
        String detalleBd = "Para avanzar con la evaluación de tu artículo '" + nombreBien + "', la casa de subastas requiere que adjuntes la documentación que acredite su origen lícito.\n\nPor favor, ingresa al detalle de tu consignación para subir los archivos correspondientes.";

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.CONSIGNACION, referenciaId);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 8. RECEPCIÓN EN SUCURSAL (Vía Script)
    // =================================================================================
    public void notificarRecepcionSucursal(Integer idCliente, String nombreBien, String sucursal, Integer referenciaId) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idCliente));

        String titulo = "Artículo Recibido";
        String mensajePush = "Tenemos tu '" + nombreBien + "' en nuestra sucursal. Iniciaremos la inspección.";
        
        String detalleBd = "Te confirmamos que hemos recibido tu artículo '" + nombreBien + "' en nuestra sucursal de " + sucursal + ".\n\nNuestros expertos procederán con la inspección física. Te notificaremos a la brevedad si el bien es aceptado para subasta y las condiciones de la misma.";

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.CONSIGNACION, referenciaId);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 9. TRANSFERENCIA ENVIADA AL DUEÑO ORIGINAL (Desde CompraService al pagar)
    // =================================================================================
    public void notificarTransferenciaEnviada(Integer idDuenio, String nombreBien, double importeNeto, String cbuDestino, Integer referenciaId) {
        Cliente duenio = clienteRepository.findById(idDuenio)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + idDuenio));

        String titulo = "Transferencia acreditada";
        String mensajePush = "El pago por tu bien '" + nombreBien + "' fue enviado a tu cuenta destino.";

        String detalleBd = String.format(
            "El comprador completó el pago por tu artículo '%s'.\n\n" +
            "Importe neto acreditado (precio venta - 10%% comisión): $%.2f\n\n" +
            "La transferencia fue enviada a la cuenta declarada con CBU/IBAN: %s",
            nombreBien, importeNeto, cbuDestino != null ? cbuDestino : "cuenta registrada"
        );

        notificacionService.crearNotificacion(duenio, titulo, detalleBd, TipoNotificacion.GANADA, referenciaId);
        firebasePushService.enviarNotificacionPush(duenio.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 10. ACEPTACIÓN DE LA OFERTA POR EL CLIENTE (Desde Backend)
    // =================================================================================
    public void notificarAceptacionOfertaCliente(Cliente cliente, String nombreBien, String fechaSubasta, Integer referenciaId) {
        String titulo = "¡Subasta Confirmada!";
        String mensajePush = "¡Genial! Tu '" + nombreBien + "' ya tiene lugar en nuestra próxima subasta.";
        
        String detalleBd = "Has aceptado exitosamente las condiciones, el valor base y las comisiones para tu artículo '" + nombreBien + "'.\n\nEl mismo ha sido formalmente incluido en el catálogo y será subastado el día " + fechaSubasta + ". Podrás seguir el evento en vivo desde la aplicación.";

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.INFORMATIVA, referenciaId);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }
}