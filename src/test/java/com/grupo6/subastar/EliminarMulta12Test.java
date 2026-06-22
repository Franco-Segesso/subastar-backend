package com.grupo6.subastar;

import com.grupo6.subastar.model.Multa;
import com.grupo6.subastar.repository.MultaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EliminarMulta12Test {

    private static final int MULTA_ID = 12;
    private static final String EMAIL_ESPERADO = "patalanogruta@gmail.com";

    @Autowired
    private MultaRepository multaRepository;

    @Test
    void eliminarMultaSolicitada() {
        Multa multa = multaRepository.findById(MULTA_ID)
                .orElseThrow(() -> new IllegalStateException("La multa 12 no existe"));

        String emailReal = multa.getCliente().getPersona().getEmail();
        assertTrue(EMAIL_ESPERADO.equalsIgnoreCase(emailReal),
                "La multa 12 pertenece a otro usuario: " + emailReal);

        multaRepository.delete(multa);
        multaRepository.flush();

        assertFalse(multaRepository.existsById(MULTA_ID),
                "La multa 12 no pudo eliminarse");
    }
}
