package edu.esi.ds.esiusuarios.services;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendWelcomeEmail(String username) {
        // Simular envío asíncrono o final del flujo
        System.out.println("[EsiUsuarios: EmailService] Simulando envío de correo de bienvenida a / confirmación de cuenta para el usuario: " + username);
    }
}
