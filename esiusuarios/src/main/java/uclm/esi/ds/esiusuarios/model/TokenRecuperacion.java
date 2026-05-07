package uclm.esi.ds.esiusuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

// Esta clase representa la tabla token_recuperacion en MySQL
// Se usa para el flujo de "olvidé mi contraseña"
@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El email del usuario que pidió recuperar la contraseña
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // El código aleatorio que se envía por email
    // Ejemplo: "a3f9k2b1-d4e5-..."
    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    // Momento en que caduca este token (en milisegundos desde 1970)
    // Ejemplo: System.currentTimeMillis() + 15*60*1000 → caduca en 15 minutos
    @Column(name = "expira_en", nullable = false)
    private Long expiraEn;

    // Constructor vacío obligatorio para Spring/JPA
    public TokenRecuperacion() {}

    // Constructor para crear un token nuevo fácilmente
    public TokenRecuperacion(String email, String token, Long expiraEn) {
        this.email = email;
        this.token = token;
        this.expiraEn = expiraEn;
    }

    // ============================================================
    // GETTERS Y SETTERS
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(Long expiraEn) {
        this.expiraEn = expiraEn;
    }

    // Método de utilidad: devuelve true si el token ya ha caducado
    // Lo usaremos en el servicio para comprobar si el código es válido
    public boolean haExpirado() {
        return System.currentTimeMillis() > this.expiraEn;
    }
}
