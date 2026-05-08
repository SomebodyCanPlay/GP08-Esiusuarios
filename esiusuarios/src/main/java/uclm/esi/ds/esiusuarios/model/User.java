package uclm.esi.ds.esiusuarios.model;

// Estas son las "etiquetas" que le dicen a Spring:
// esta clase representa una tabla en la base de datos
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

// @Entity → le dice a Spring que esta clase es una tabla de la BD
// @Table(name = "usuario") → le dice el nombre exacto de la tabla en MySQL
@Entity
@Table(name = "usuario")
public class User {

    // @Id → esta campo es la clave primaria
    // @GeneratedValue → MySQL le pone el número automáticamente (AUTO_INCREMENT)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column → conecta este campo con la columna de MySQL
    // unique = true → no puede haber dos usuarios con el mismo email
    // nullable = false → obligatorio, no puede estar vacío
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    // La contraseña NUNCA se guarda en texto plano.
    // Aquí guardaremos el resultado de cifrarla con BCrypt.
    // Ejemplo: "1234" → "$2a$10$xKG..." (un código irreversible)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // El nombre del usuario (ej: "Eduardo")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Token que se genera cuando el usuario hace login correctamente.
    // Este token es el que esientradas le pedirá para confirmar que el usuario está
    // logueado.
    // Puede ser null si el usuario no ha hecho login todavía.
    @Column(name = "token_sesion", length = 100)
    private String tokenSesion;

    // true = cuenta activa, false = cuenta cancelada
    // @Column(columnDefinition = ...) le dice a Hibernate el tipo exacto en MySQL
    @Column(name = "activo", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean activo = true;

    // Constructor vacío: Spring lo necesita obligatoriamente para crear objetos
    // desde la BD
    public User() {
    }

    // Constructor para registrar un usuario nuevo
    public User(String email, String passwordHash, String nombre) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = true;
    }

    // ============================================================
    // GETTERS Y SETTERS (métodos para leer y escribir cada campo)
    // ============================================================

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTokenSesion() {
        return tokenSesion;
    }

    public void setTokenSesion(String tokenSesion) {
        this.tokenSesion = tokenSesion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
