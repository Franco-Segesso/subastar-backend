package com.grupo6.subastar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.model.TipoNotificacion;

@Service
public class NotificacionesReactivasService {

    @Autowired
    private FirebasePushService firebasePushService;
    @Autowired
    private NotificacionService notificacionService;
    
    // @Autowired
    // private NotificacionService notificacionService; // El que guarda en tu BD de Azure

    // =================================================================================
    // 1. EL CLIENTE GANA EN LA SALA DE PUJA
    // =================================================================================
    public void notificarSubastaGanada(Cliente cliente, String nombreItem, double pujaGanadora, double comisiones, double costoEnvio, Integer referenciaId) {
        double totalAPagar = pujaGanadora + comisiones + costoEnvio;
        
        String titulo = "¡Subasta Ganada!";
        String mensajePush = "Ganaste '" + nombreItem + "'. Total a pagar: $" + totalAPagar + ". Toca para ver el detalle.";
        
        String detalleBd = String.format(
            "¡Felicidades! Eres el nuevo dueño de '%s'.\n\nDesglose de pago:\n- Valor pujado: $%.2f\n- Comisiones: $%.2f\n- Costo de envío: $%.2f\n\nTOTAL A ABONAR: $%.2f",
            nombreItem, pujaGanadora, comisiones, costoEnvio, totalAPagar
        );

        // Guardamos en la base de datos (con el ID de la compra/ítem ganado como referencia)
        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.GANADA, referenciaId);

        // Disparamos la Push
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 2. EL CLIENTE CONSIGNA UN BIEN (Enviado a revisión)
    // =================================================================================
    public void notificarConsignacionEnviada(Cliente cliente, String nombreBien, Integer referenciaId) {
        String titulo = "Consignación en Revisión";
        String mensaje = "Hemos recibido el formulario de tu artículo '" + nombreBien + "'. Nuestros expertos lo están evaluando.";

        // 1. Guardamos en la base de datos de Azure (ahora le pasamos el objeto Cliente entero y la referencia)
        notificacionService.crearNotificacion(cliente, titulo, mensaje, TipoNotificacion.INFORMATIVA, referenciaId);
        
        // 2. Disparamos la Push al celular
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensaje);
    }

    // =================================================================================
    // 3. EL BIEN ES RECHAZADO POR LA CASA (Vía Script)
    // =================================================================================
    public void notificarBienRechazado(Cliente cliente, String nombreBien, String motivoRechazo) {
        String titulo = "Actualización de Consignación";
        String mensajePush = "Lamentablemente tu artículo '" + nombreBien + "' no fue aceptado. Toca para ver los detalles.";
        
        // La consigna exige informar las causas del rechazo y recordar que la devolución tiene cargo
        String detalleBd = "Tras la inspección, tu artículo '" + nombreBien + "' ha sido rechazado.\n\nMotivo: " + motivoRechazo + 
                           "\n\nTe recordamos que, tal como aceptaste en los términos, el bien te será devuelto a tu domicilio con cargo a tu cuenta.";

        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.CONSIGNACION, null);
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 4. EL BIEN ES ACEPTADO - SE LE ENVÍA LA OFERTA AL CLIENTE (Vía Script)
    // =================================================================================
    public void notificarOfertaDeLaCasa(Integer idCliente, String nombreBien, double valorBase, double comisionPorcentaje, String fecha, String hora, String lugar) {
        String titulo = "¡Artículo Aceptado!";
        String mensajePush = "Tenemos una propuesta para subastar tu '" + nombreBien + "'. Revisa los términos.";
        
        // La consigna exige informar fecha, hora, lugar, valor base y comisiones
        String detalleBd = String.format(
            "¡Tu artículo '%s' ha sido aprobado para subasta!\n\nCondiciones propuestas:\n- Valor Base: $%.2f\n- Comisión de la casa: %.1f%%\n\nLa subasta se realizará el %s a las %s en %s.\n\nPor favor, ingresa a la app para ACEPTAR o RECHAZAR estas condiciones.",
            nombreBien, valorBase, comisionPorcentaje, fecha, hora, lugar
        );

        // notificacionService.crearNotificacion(idCliente, titulo, detalleBd, TipoNotificacion.ACCION_REQUERIDA);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 5. EL CLIENTE RECHAZA LA OFERTA DE LA CASA
    // =================================================================================
    public void notificarOfertaRechazadaPorCliente(Cliente cliente, String nombreBien, String sucursalRetiro, double cargoDevolucion, Integer referenciaId) {
        String titulo = "Devolución de Artículo";
        String mensajePush = "Has rechazado nuestra propuesta para '" + nombreBien + "'.";
        
        String detalleBd = String.format(
            "Lamentamos que no hayas aceptado la propuesta de valor base y comisiones para tu '%s'.\n\nPuedes pasar a retirar tu artículo por nuestra sucursal ubicada en %s.\nRecuerda que debes abonar un cargo operativo de $%.2f al momento del retiro.",
            nombreBien, sucursalRetiro, cargoDevolucion
        );

        // Guardamos en la base de datos
        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.INFORMATIVA, referenciaId);
        
        // Disparamos la Push al celular
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }

    // =================================================================================
    // 6. PEDIDO DE DOCUMENTACIÓN (Ejecutado por el Script)
    // =================================================================================
    public void notificarPedidoDocumentacion(Integer idCliente, String nombreBien) {
        String titulo = "Documentación Requerida";
        String mensajePush = "Necesitamos verificar el origen de '" + nombreBien + "'. Toca para más info.";
        
        // El TP exige "poder acreditar el origen licito de los bienes"
        String detalleBd = "Para avanzar con la evaluación de tu artículo '" + nombreBien + "', la casa de subastas requiere que adjuntes la documentación que acredite su origen lícito.\n\nPor favor, ingresa al detalle de tu consignación para subir los archivos correspondientes.";

        // notificacionService.crearNotificacion(idCliente, titulo, detalleBd, TipoNotificacion.ACCION_REQUERIDA);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 7. RECEPCIÓN EN SUCURSAL (Ejecutado por el Script)
    // =================================================================================
    public void notificarRecepcionSucursal(Integer idCliente, String nombreBien, String sucursal) {
        String titulo = "Artículo Recibido";
        String mensajePush = "Tenemos tu '" + nombreBien + "' en nuestra sucursal. Iniciaremos la inspección.";
        
        String detalleBd = "Te confirmamos que hemos recibido tu artículo '" + nombreBien + "' en nuestra sucursal de " + sucursal + ".\n\nNuestros expertos procederán con la inspección física. Te notificaremos a la brevedad si el bien es aceptado para subasta y las condiciones de la misma.";

        // notificacionService.crearNotificacion(idCliente, titulo, detalleBd, TipoNotificacion.INFO);
        firebasePushService.enviarNotificacionPush(idCliente, titulo, mensajePush);
    }

    // =================================================================================
    // 8. ACEPTACIÓN DE LA OFERTA POR EL CLIENTE (Confirmación final)
    // =================================================================================
    public void notificarAceptacionOfertaCliente(Cliente cliente, String nombreBien, String fechaSubasta, Integer referenciaId) {
        String titulo = "¡Subasta Confirmada!";
        String mensajePush = "¡Genial! Tu '" + nombreBien + "' ya tiene lugar en nuestra próxima subasta.";
        
        String detalleBd = "Has aceptado exitosamente las condiciones, el valor base y las comisiones para tu artículo '" + nombreBien + "'.\n\nEl mismo ha sido formalmente incluido en el catálogo y será subastado el día " + fechaSubasta + ". Podrás seguir el evento en vivo desde la aplicación.";

        // Guardamos en la base de datos
        notificacionService.crearNotificacion(cliente, titulo, detalleBd, TipoNotificacion.INFORMATIVA, referenciaId);
        
        // Disparamos la Push al celular
        firebasePushService.enviarNotificacionPush(cliente.getIdentificador(), titulo, mensajePush);
    }
}