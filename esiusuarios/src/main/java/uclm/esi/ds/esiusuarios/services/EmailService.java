package uclm.esi.ds.esiusuarios.services;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// @Service → Spring gestiona esta clase automáticamente
@Service
public class EmailService {

    // JavaMailSender es la herramienta de Spring para enviar emails
    // Spring la configura automáticamente con los datos de application.properties
    // (spring.mail.host, spring.mail.username, etc.)
    @Autowired
    private JavaMailSender mailSender;

    // @Value lee el valor de "app.mail.from" del application.properties
    // En tu caso (ej): javier@gmail.com
    @Value("${app.mail.from}")
    private String emailFrom;

    // ============================================================
    // Email de bienvenida al registrarse
    // ============================================================
    @Async // Esto hace que el método se ejecute en un hilo separado
    public void sendWelcomeEmail(String emailDestino, String nombre) {
        try {
            // MimeMessage permite usar nombres personalizados en el remitente
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "utf-8");

            // Quién lo envía: correo real y el nombre que verá el usuario
            helper.setFrom("edugallego23@gmail.com", "ESIentradas");
            helper.setTo(emailDestino); // A quién va
            helper.setSubject("¡Bienvenido a ESIentradas!");
            helper.setText(
                    "Hola " + nombre + ",\n\n" +
                            "Tu cuenta en ESIentradas ha sido creada correctamente.\n" +
                            "Ya puedes buscar espectáculos y comprar entradas.\n\n" +
                            "¡Que disfrutes del espectáculo!\n" +
                            "El equipo de ESIentradas");

            // Enviar el email por Brevo
            mailSender.send(mensaje);

            System.out.println("[EmailService] Email de bienvenida enviado a: " + emailDestino);

        } catch (Exception e) {
            // Si falla el email, lo logamos pero NO rompemos el registro del usuario
            System.err.println("[EmailService] Error enviando email de bienvenida: " + e.getMessage());
        }
    }

    // ============================================================
    // Email de recuperación de contraseña
    // ============================================================
    @Async
    public void sendPasswordRecoveryEmail(String emailDestino, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "utf-8");

            helper.setFrom("edugallego23@gmail.com", "ESIentradas");
            helper.setTo(emailDestino);
            helper.setSubject("Recuperación de contraseña - ESIentradas");
            helper.setText(
                    "Hola,\n\n" +
                            "Has solicitado recuperar tu contraseña en ESIentradas.\n\n" +
                            "Tu código de recuperación es:\n\n" +
                            "   " + codigo + "\n\n" +
                            "Este código caduca en 15 minutos.\n" +
                            "Si no has solicitado esto, ignora este email.\n\n" +
                            "El equipo de ESIentradas");

            mailSender.send(mensaje);

            System.out.println("[EmailService] Email de recuperación enviado a: " + emailDestino);

        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando email de recuperación: " + e.getMessage());
        }
    }

    // ============================================================
    // Email de confirmación de cuenta cancelada (baja)
    // ============================================================
    @Async
    public void sendAccountCancellationEmail(String emailDestino) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "utf-8");

            helper.setFrom("edugallego23@gmail.com", "ESIentradas");
            helper.setTo(emailDestino);
            helper.setSubject("Confirmación de baja - ESIentradas");
            helper.setText(
                    "Hola,\n\n" +
                            "Te confirmamos que tu cuenta en ESIentradas ha sido cancelada correctamente.\n" +
                            "Todos tus datos de sesión han sido invalidados por seguridad.\n\n" +
                            "Lamentamos que nos dejes. Si en el futuro cambias de opinión, siempre serás bienvenido a registrarte de nuevo en nuestra taquilla virtual.\n\n"
                            +
                            "Un cordial saludo,\n" +
                            "El equipo de ESIentradas");

            mailSender.send(mensaje);

            System.out.println("[EmailService] Email de confirmación de baja enviado a: " + emailDestino);

        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando email de baja: " + e.getMessage());
        }
    }
}