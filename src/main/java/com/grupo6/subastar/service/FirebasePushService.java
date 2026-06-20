package com.grupo6.subastar.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushService {

    public void enviarNotificacionPush(Integer clienteId, String titulo, String mensaje) {
        String topic = "cliente_" + clienteId;

        Message pushMessage = Message.builder()
                .putData("titulo", titulo)
                .putData("mensaje", mensaje)
                .setTopic(topic)
                .build();

        try {
            System.out.println(">> Intentando enviar push al canal: " + topic);
            
            // CAMBIO CLAVE: Usamos send() síncrono para que frene y nos dé la respuesta exacta
            String response = FirebaseMessaging.getInstance().send(pushMessage);
            
            System.out.println("✅ EXITO FIREBASE: Mensaje entregado a los servidores de Google. ID: " + response);
        } catch (Exception e) {
            System.err.println("❌ ERROR FIREBASE: Google rechazó el mensaje. Motivo exacto:");
            e.printStackTrace();
        }
    }
}