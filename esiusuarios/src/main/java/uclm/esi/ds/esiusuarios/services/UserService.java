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

        // Si la cuenta ya está activa, no hacemos nada más para no reenviar el email.
        // Simplemente borramos el token y terminamos.
        if (user.isActivo()) {
            tokenConfirmacionRepository.delete(token);
            return;
        }

        user.setActivo(true);
        userDao.save(user);

        // Una vez activada, enviamos el email de bienvenida.
        emailService.sendWelcomeEmail(user.getEmail(), user.getNombre());

        // Borramos el token para que no se pueda volver a usar.
        tokenConfirmacionRepository.delete(token);
    }

    // --> MÉTODO PRIVADO NUEVO para no repetir código
    private void enviarCorreoDeActivacion(User user) {
        String tokenValue = UUID.randomUUID().toString();
        TokenConfirmacion token = new TokenConfirmacion(tokenValue, user.getEmail());
        tokenConfirmacionRepository.save(token);
        emailService.enviarCorreoConfirmacion(user.getEmail(), tokenValue);
    }
}
