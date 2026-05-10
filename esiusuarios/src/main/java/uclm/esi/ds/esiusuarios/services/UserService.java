package uclm.esi.ds.esiusuarios.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import uclm.esi.ds.esiusuarios.dao.UserDao;
import uclm.esi.ds.esiusuarios.dao.TokenRecuperacionDao;
import uclm.esi.ds.esiusuarios.model.User;
import uclm.esi.ds.esiusuarios.model.TokenRecuperacion;

import java.util.Optional;
import java.util.UUID;

@Service // Lógica del servicio de usuarios
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenRecuperacionDao tokenRecuperacionDao;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Registrar usuario
    public String register(String email, String password, String nombre) {

        if (email == null || email.isBlank() ||
                password == null || password.isBlank() ||
                nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Email, contraseña y nombre son obligatorios");
        }

        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        String passwordCifrada = encoder.encode(password);
        User nuevoUsuario = new User(email, passwordCifrada, nombre);
        userDao.save(nuevoUsuario);

        emailService.sendWelcomeEmail(email, nombre);

        return "Usuario registrado correctamente";
    }

    // Login y generación de token
    @Transactional
    public String login(String email, String password) {

        Optional<User> optUser = userDao.findByEmail(email);

        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        User user = optUser.get();

        if (!user.isActivo()) {
            throw new IllegalArgumentException("Esta cuenta está cancelada");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
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
            return;
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

        Optional<TokenRecuperacion> optToken = tokenRecuperacionDao.findByToken(codigo);

        if (optToken.isEmpty()) {
            throw new IllegalArgumentException("Código de recuperación no válido");
        }

        TokenRecuperacion tokenRec = optToken.get();

        if (tokenRec.haExpirado()) {
            tokenRecuperacionDao.delete(tokenRec);
            throw new IllegalArgumentException("El código ha caducado. Solicita uno nuevo.");
        }

        Optional<User> optUser = userDao.findByEmail(tokenRec.getEmail());
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        User user = optUser.get();
        user.setPasswordHash(encoder.encode(nuevaPassword));
        userDao.save(user);

        tokenRecuperacionDao.delete(tokenRec);
    }

    // Cancelar cuenta y limpieza
    @Transactional
    public void cancelarCuenta(String email, String password) {

        Optional<User> optUser = userDao.findByEmail(email);
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        User user = optUser.get();

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        tokenRecuperacionDao.deleteByEmail(email);

        userDao.delete(user);

        emailService.sendAccountCancellationEmail(email);
    }
}
