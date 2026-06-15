package uclm.esi.ds.esiusuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class TokenConfirmacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;
    private String userEmail;
    private long expiraEn; // Timestamp de expiración

    public TokenConfirmacion() {}

    public TokenConfirmacion(String token, String userEmail) {
        this.token = token;
        this.userEmail = userEmail;
        // El token expira en 24 horas
        this.expiraEn = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
    }

    // --- Getters ---
    public String getToken() { return token; }
    public String getUserEmail() { return userEmail; }
    public long getExpiraEn() { return expiraEn; }
}
