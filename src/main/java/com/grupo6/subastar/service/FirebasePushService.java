package com.grupo6.subastar.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushService {

    public void enviarNotificacionPush(Integer clienteId, String titulo, String mensaje) {
        // Le enviamos al "Tópico" al que se suscribió el celular
        String topic = "cliente_" + clienteId;

        Message pushMessage = Message.builder()
                .putData("titulo", titulo)
                .putData("mensaje", mensaje)
                .setTopic(topic)
                .build();

        try {
            FirebaseMessaging.getInstance().sendAsync(pushMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}