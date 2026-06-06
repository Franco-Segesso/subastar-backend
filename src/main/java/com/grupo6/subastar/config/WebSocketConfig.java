package com.grupo6.subastar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un broker en memoria para que los clientes (Android) se suscriban a los tópicos
        config.enableSimpleBroker("/topic");
        // Prefijo para los mensajes que el cliente envíe al servidor (si fuera necesario en el futuro)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint al que se conectará Android (Retrofit/OkHttp) para iniciar el handshake
        registry.addEndpoint("/subastar-ws").setAllowedOriginPatterns("*").withSockJS();
    }
}