package com.grupo6.subastar.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

// CLAVE: Cambiado de javax a jakarta para que Spring Boot 3 lo reconozca
import jakarta.annotation.PostConstruct; 
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // Ponemos carteles gigantes para verlos de inmediato en la consola
            System.out.println("=========================================");
            System.out.println(">> ¡ARRANCANDO LA CONFIGURACIÓN DE FIREBASE!");
            System.out.println("=========================================");
            
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-admin-key.json");
            
            if (serviceAccount == null) {
                System.err.println("ERROR: El archivo 'firebase-admin-key.json' no existe en src/main/resources/");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase inicializado con éxito en el Servidor.");
            }
            System.out.println("=========================================");
        } catch (Exception e) {
            System.err.println("Error en la inicialización: " + e.getMessage());
            e.printStackTrace();
        }
    }
}