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

// @Service → le dice a Spring que esta clase contiene la lógica del negocio
// Spring la gestiona automáticamente y permite inyectarla con @Autowired
@Service
public class UserService {

    // @Autowired → Spring "inyecta" automáticamente el DAO aquí
    // No necesitas hacer "new UserDao()" — Spring lo hace por ti
    @Autowired
    private UserDao userDao;

    @Autowired
    private TokenRecuperacionDao tokenRecuperacionDao;

    @Autowired
    private EmailService emailService;

    // BCryptPasswordEncoder es la herramienta que cifra contraseñas
    // Ejemplo: "1234" → "$2a$10$xKG..." (un hash irreversible)
    // Lo creamos como campo para reutilizarlo en todos los métodos
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ============================================================
    // REGISTRO DE USUARIO NUEVO
    // ============================================================
    // Recibe el email, la contraseña (en texto plano) y el nombre
    // Devuelve un mensaje de éxito o lanza excepción si hay error
    public String register(String email, String password, String nombre) {

        // Validación básica: comprobar que no vienen vacíos
        if (email == null || email.isBlank() ||
                password == null || password.isBlank() ||
                nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Email, contraseña y nombre son obligatorios");
        }

        // Comprobar que no existe ya un usuario con ese email
        // userDao.findByEmail devuelve Optional → isPresent() = true si lo encontró
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        // Cifrar la contraseña ANTES de guardarla
        // encoder.encode("1234") devuelve algo como "$2a$10$xKG..."
        // Así si alguien accede a la BD, no puede ver las contraseñas reales
        String passwordCifrada = encoder.encode(password);

        // Crear el objeto User con los datos y guardarlo en MySQL
        User nuevoUsuario = new User(email, passwordCifrada, nombre);
        userDao.save(nuevoUsuario); // save() viene gratis de JpaRepository

        // Enviar email de bienvenida (lo hace EmailService con Brevo)
        emailService.sendWelcomeEmail(email, nombre);

        return "Usuario registrado correctamente";
    }

    // ============================================================
    // LOGIN (iniciar sesión)
    // ============================================================
    // Recibe email y contraseña en texto plano
    // Si son correctos, genera un token UUID, lo guarda en BD y lo devuelve
    // El token es lo que esientradas nos pedirá para confirmar que el usuario está
    // logueado
    @Transactional
    public String login(String email, String password) {

        // Buscar el usuario por email
        Optional<User> optUser = userDao.findByEmail(email);

        // Si no existe, lanzar error
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        User user = optUser.get();

        // Comprobar que la cuenta está activa
        if (!user.isActivo()) {
            throw new IllegalArgumentException("Esta cuenta está cancelada");
        }

        // encoder.matches("1234", "$2a$10$xKG...") → compara texto plano con el hash
        // guardado
        // Devuelve true si la contraseña es correcta, false si no
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos");
        }

        // Generar un token UUID único para esta sesión
        // UUID.randomUUID().toString() genera algo como "a3f9k2b1-d4e5-..."
        String nuevoToken = UUID.randomUUID().toString();

        // Guardar el token en la BD (en la columna token_sesion del usuario)
        userDao.actualizarTokenSesion(email, nuevoToken);

        // Devolver el token → el frontend lo guardará y lo enviará a esientradas al
        // comprar
        return nuevoToken;
    }

    // ============================================================
    // LOGOUT (cerrar sesión)
    // ============================================================
    // Busca el usuario que tiene ese token y lo pone a null en la BD.
    // Así el token deja de ser válido aunque alguien lo tenga guardado.
    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank())
            return;
        // Buscamos el usuario que tiene ese token activo
        userDao.findByTokenSesion(token).ifPresent(user -> {
            user.setTokenSesion(null);
            userDao.save(user);
        });
    }

    // ============================================================
    // VALIDAR TOKEN (para que esientradas compruebe si el usuario está logueado)
    // ============================================================
    // Recibe el token que el usuario tiene guardado
    // Devuelve el email del usuario si el token es válido, null si no existe
    public String checkToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        // Buscar en la BD un usuario con ese token de sesión
        Optional<User> optUser = userDao.findByTokenSesion(token);

        // Si existe y está activo, devolver su email
        if (optUser.isPresent() && optUser.get().isActivo()) {
            return optUser.get().getEmail();
        }

        return null; // Token no válido
    }

    // ============================================================
    // SOLICITAR RECUPERACIÓN DE CONTRASEÑA
    // ============================================================
    // El usuario dice "olvidé mi contraseña" y da su email
    // Generamos un código, lo guardamos en BD y lo enviamos por email con Brevo
    @Transactional
    public void solicitarRecuperacion(String email) {

        // Comprobar que existe un usuario con ese email
        Optional<User> optUser = userDao.findByEmail(email);
        if (optUser.isEmpty()) {
            // Por seguridad, no decimos si el email existe o no
            // (para que no se pueda descubrir qué emails están registrados)
            return;
        }

        // Borrar tokens viejos de este email (por si pidió recuperación antes)
        tokenRecuperacionDao.deleteByEmail(email);

        // Generar el código de recuperación (un UUID sin guiones, más corto)
        String codigo = UUID.randomUUID().toString().replace("-", "");

        // Calcular cuándo caduca: ahora + 15 minutos (en milisegundos)
        // System.currentTimeMillis() devuelve el tiempo actual en milisegundos
        // 15 * 60 * 1000 = 900.000 milisegundos = 15 minutos
        long expiraEn = System.currentTimeMillis() + (15 * 60 * 1000);

        // Guardar el token en la BD
        TokenRecuperacion tokenRec = new TokenRecuperacion(email, codigo, expiraEn);
        tokenRecuperacionDao.save(tokenRec);

        // Enviar el código por email con Brevo
        emailService.sendPasswordRecoveryEmail(email, codigo);
    }

    // ============================================================
    // RESTABLECER CONTRASEÑA (con el código recibido por email)
    // ============================================================
    @Transactional
    public void restablecerPassword(String codigo, String nuevaPassword) {

        // Buscar el token en la BD
        Optional<TokenRecuperacion> optToken = tokenRecuperacionDao.findByToken(codigo);

        if (optToken.isEmpty()) {
            throw new IllegalArgumentException("Código de recuperación no válido");
        }

        TokenRecuperacion tokenRec = optToken.get();

        // Comprobar que no ha caducado
        // haExpirado() devuelve true si ya pasaron los 15 minutos
        if (tokenRec.haExpirado()) {
            tokenRecuperacionDao.delete(tokenRec); // Limpiamos el token caducado
            throw new IllegalArgumentException("El código ha caducado. Solicita uno nuevo.");
        }

        // Buscar al usuario por email
        Optional<User> optUser = userDao.findByEmail(tokenRec.getEmail());
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        User user = optUser.get();

        // Cifrar la nueva contraseña y guardarla
        user.setPasswordHash(encoder.encode(nuevaPassword));
        userDao.save(user);

        // Borrar el token ya usado (no se puede usar dos veces)
        tokenRecuperacionDao.delete(tokenRec);
    }

    // ============================================================
    // CANCELAR CUENTA
    // ============================================================
    @Transactional
    public void cancelarCuenta(String email, String password) {

        Optional<User> optUser = userDao.findByEmail(email);
        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        User user = optUser.get();

        // Verificar la contraseña antes de cancelar (por seguridad)
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Contraseña incorrecta");
        }

        // 1. Borramos cualquier token de recuperación que tenga este email
        tokenRecuperacionDao.deleteByEmail(email);

        // 2. Borramos al usuario de la base de datos (BORRADO TOTAL)
        userDao.delete(user);

        // 3. Enviamos el correo de despedida
        emailService.sendAccountCancellationEmail(email);
    }
}
