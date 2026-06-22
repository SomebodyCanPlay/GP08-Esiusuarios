package uclm.esi.ds.esiusuarios.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import uclm.esi.ds.esiusuarios.dao.TokenConfirmacionRepository;
import uclm.esi.ds.esiusuarios.dao.TokenRecuperacionDao;
import uclm.esi.ds.esiusuarios.dao.UserDao;
import uclm.esi.ds.esiusuarios.model.TokenConfirmacion;
import uclm.esi.ds.esiusuarios.model.TokenRecuperacion;
import uclm.esi.ds.esiusuarios.model.User;

@Service // Lógica del servicio de usuarios
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenRecuperacionDao tokenRecuperacionDao;

    // --> INYECCIÓN DEL NUEVO REPOSITORIO
    @Autowired
    private TokenConfirmacionRepository tokenConfirmacionRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // --> MÉTODO REGISTER MODIFICADO
    @Transactional
    public String register(String email, String password, String nombre) {

        if (email == null || email.isBlank() || password == null || password.isBlank() || nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email, contraseña y nombre son obligatorios");
        }

        if (!esPasswordRobusta(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no cumple con los requisitos mínimos de seguridad.");
        }

        if (userDao.findByEmail(email).isPresent()) {
            // Devolvemos un 409 Conflict, que es más apropiado. El frontend ya lo gestiona.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");
        }

        String passwordCifrada = encoder.encode(password);
        User nuevoUsuario = new User(email, passwordCifrada, nombre); // Se crea como inactivo por defecto
        userDao.save(nuevoUsuario);

        // Ya no se envía el email de bienvenida, sino el de activación
        enviarCorreoDeActivacion(nuevoUsuario);

        return "Usuario registrado correctamente";
    }

    private boolean esPasswordRobusta(String pwd) {
        if (pwd.length() < 8) return false;
        if (!pwd.matches(".*[A-ZÁÉÍÓÚÜÑ].*")) return false; // Al menos una mayúscula
        if (!pwd.matches(".*[a-záéíóúüñ].*")) return false; // Al menos una minúscula
        if (!pwd.matches(".*[\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~].*")) return false; // Al menos un número o especial
        return true;
    }

    // --> MÉTODO LOGIN MODIFICADO
    @Transactional
    public String login(String email, String password) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }

        // Si el usuario no está activo, reenviamos el correo y avisamos al frontend
        if (!user.isActivo()) {
            enviarCorreoDeActivacion(user);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no ha sido activada. Se ha reenviado el correo de confirmación.");
        }

        String nuevoToken = UUID.randomUUID().toString();
        userDao.actualizarTokenSesion(email, nuevoToken);
        return nuevoToken;
    }

    // Cerrar sesión
    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank())
            return;
        userDao.findByTokenSesion(token).ifPresent(user -> {
            user.setTokenSesion(null);
            userDao.save(user);
        });
    }

    // Comprobar token y devolver email si válido
    public String checkToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Optional<User> optUser = userDao.findByTokenSesion(token);
        if (optUser.isPresent() && optUser.get().isActivo()) {
            return optUser.get().getEmail();
        }
        return null;
    }

    // Solicitar recuperación de contraseña
    @Transactional
    public void solicitarRecuperacion(String email) {
        Optional<User> optUser = userDao.findByEmail(email);
        if (optUser.isEmpty()) {
            return; // No revelamos si el email existe
        }
        tokenRecuperacionDao.deleteByEmail(email);
        String codigo = UUID.randomUUID().toString().replace("-", "");
        long expiraEn = System.currentTimeMillis() + (15 * 60 * 1000);
        TokenRecuperacion tokenRec = new TokenRecuperacion(email, codigo, expiraEn);
        tokenRecuperacionDao.save(tokenRec);
        emailService.sendPasswordRecoveryEmail(email, codigo);
    }

    // Restablecer contraseña con código
    @Transactional
    public void restablecerPassword(String codigo, String nuevaPassword) {
        TokenRecuperacion tokenRec = tokenRecuperacionDao.findByToken(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de recuperación no válido"));

        if (tokenRec.haExpirado()) {
            tokenRecuperacionDao.delete(tokenRec);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código ha caducado. Solicita uno nuevo.");
        }

        if (!esPasswordRobusta(nuevaPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no cumple con los requisitos mínimos de seguridad.");
        }

        User user = userDao.findByEmail(tokenRec.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setPasswordHash(encoder.encode(nuevaPassword));
        userDao.save(user);
        tokenRecuperacionDao.delete(tokenRec);
    }

    // Cancelar cuenta y limpieza
    @Transactional
    public void cancelarCuenta(String email, String password) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña incorrecta");
        }
        tokenRecuperacionDao.deleteByEmail(email);
        userDao.delete(user);
        emailService.sendAccountCancellationEmail(email);
    }

    // --> MÉTODO NUEVO para activar la cuenta
    @Transactional
    public void confirmarCuenta(String tokenValue) {
        TokenConfirmacion token = tokenConfirmacionRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de confirmación no es válido."));

        if (token.getExpiraEn() < System.currentTimeMillis()) {
            tokenConfirmacionRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de confirmación ha caducado.");
        }

        User user = userDao.findByEmail(token.getUserEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario asociado al token no encontrado."));

        if (user.isActivo()) {
            tokenConfirmacionRepository.delete(token);
            return;
        }

        user.setActivo(true);
        userDao.save(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getNombre());

        tokenConfirmacionRepository.delete(token);
    }

    // --> MÉTODO PRIVADO NUEVO para no repetir código
    private void enviarCorreoDeActivacion(User user) {
        String tokenValue = UUID.randomUUID().toString();
        TokenConfirmacion token = new TokenConfirmacion(tokenValue, user.getEmail());
        tokenConfirmacionRepository.save(token);
        emailService.enviarCorreoConfirmacion(user.getEmail(), tokenValue);
    }

    // =========================================
    // MÉTODOS DEL MONEDERO (NUEVO)
    // =========================================

    // Obtener saldo actual
    public Double obtenerSaldo(String token) {
        User user = userDao.findByTokenSesion(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no válido"));
        return user.getSaldoMonedero();
    }

    // Sumar saldo al monedero (cuando se cancela una entrada)
    @Transactional
    public void sumarSaldo(String email, Double cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad a sumar debe ser mayor que cero");
        }
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setSaldoMonedero(user.getSaldoMonedero() + cantidad);
        userDao.save(user);
    }
    
    // Restar saldo del monedero (para cuando compre usando el saldo)
    @Transactional
    public void restarSaldo(String email, Double cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad a restar debe ser mayor que cero");
        }
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (user.getSaldoMonedero() < cantidad) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente en el monedero");
        }

        user.setSaldoMonedero(user.getSaldoMonedero() - cantidad);
        userDao.save(user);
    }
}