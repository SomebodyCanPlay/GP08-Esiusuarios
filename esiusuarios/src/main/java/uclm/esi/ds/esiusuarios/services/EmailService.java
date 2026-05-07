package uclm.esi.ds.esiusuarios.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

// @Service → Spring gestiona esta clase automáticamente
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Dirección email real de envío (configurada en application.properties)
    @Value("${app.mail.from}")
    private String emailFrom;

    // Nombre visible del remitente — lo que verá el usuario en su bandeja de entrada
    // Antes aparecía el email crudo; ahora aparece "ESIentradas"
    private static final String NOMBRE_REMITENTE = "ESIentradas";

    // ============================================================
    // Email de bienvenida al registrarse
    // ============================================================
    public void sendWelcomeEmail(String emailDestino, String nombre) {
        try {
            // MimeMessage permite personalizar el nombre del remitente
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");

            // setFrom(email, nombre) → en la bandeja del usuario aparece "ESIentradas <aa8e58001@...>"
            helper.setFrom(emailFrom, NOMBRE_REMITENTE);
            helper.setTo(emailDestino);
            helper.setSubject("¡Bienvenido a ESIentradas!");
            helper.setText(
                "Hola " + nombre + ",\n\n" +
                "Tu cuenta en ESIentradas ha sido creada correctamente.\n" +
                "Ya puedes buscar espectáculos y comprar entradas.\n\n" +
                "¡Que disfrutes del espectáculo!\n" +
                "El equipo de ESIentradas"
            );

            mailSender.send(mensaje);
            System.out.println("[EmailService] Email de bienvenida enviado a: " + emailDestino);

        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando email de bienvenida: " + e.getMessage());
        }
    }

    // ============================================================
    // Email de recuperación de contraseña
    // ============================================================
    public void sendPasswordRecoveryEmail(String emailDestino, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");

            helper.setFrom(emailFrom, NOMBRE_REMITENTE);
            helper.setTo(emailDestino);
            helper.setSubject("Recuperación de contraseña - ESIentradas");
            helper.setText(
                "Hola,\n\n" +
                "Has solicitado recuperar tu contraseña en ESIentradas.\n\n" +
                "Tu código de recuperación es:\n\n" +
                "   " + codigo + "\n\n" +
                "Este código caduca en 15 minutos.\n" +
                "Si no has solicitado esto, ignora este email.\n\n" +
                "El equipo de ESIentradas"
            );

            mailSender.send(mensaje);
            System.out.println("[EmailService] Email de recuperación enviado a: " + emailDestino);

        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando email de recuperación: " + e.getMessage());
        }
    }
}
